package com.signalgate.multipoint.data.security

import java.util.BitSet

class BloomFilterEngine(
    private val expectedElements: Int = 500000,
    private val falsePositiveRate: Double = 0.02
) {
    private val numBits: Int
    private val numHashFunctions: Int
    private val bitSet: BitSet
    private val lock = Any()

    init {
        val m = (-expectedElements * Math.log(falsePositiveRate) / Math.pow(Math.log(2.0), 2.0)).toInt()
        numBits = if (m <= 0) 1000 else m
        val k = Math.max(1, Math.round((numBits.toDouble() / expectedElements) * Math.log(2.0)).toInt())
        numHashFunctions = k
        bitSet = BitSet(numBits)
    }

    private fun murmurHash3(data: ByteArray, seed: Int): Int {
        var h1 = seed
        val length = data.size
        val nblocks = length / 4

        for (i in 0 until nblocks) {
            val index = i * 4
            var k1 = (data[index].toInt() and 0xFF) or
                    ((data[index + 1].toInt() and 0xFF) shl 8) or
                    ((data[index + 2].toInt() and 0xFF) shl 16) or
                    ((data[index + 3].toInt() and 0xFF) shl 24)

            k1 *= 0xcc9e2d51.toInt()
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= 0x1b873593.toInt()

            h1 = h1 xor k1
            h1 = Integer.rotateLeft(h1, 13)
            h1 = h1 * 5 + 0xe6546b64.toInt()
        }

        var k1 = 0
        val tailStart = nblocks * 4
        val left = length - tailStart

        if (left >= 3) k1 = k1 xor ((data[tailStart + 2].toInt() and 0xFF) shl 16)
        if (left >= 2) k1 = k1 xor ((data[tailStart + 1].toInt() and 0xFF) shl 8)
        if (left >= 1) {
            k1 = k1 xor (data[tailStart].toInt() and 0xFF)
            k1 *= 0xcc9e2d51.toInt()
            k1 = Integer.rotateLeft(k1, 15)
            k1 *= 0x1b873593.toInt()
            h1 = h1 xor k1
        }

        h1 = h1 xor length
        h1 = h1 xor (h1 ushr 16)
        h1 *= -0x7a143595.toInt()
        h1 = h1 xor (h1 ushr 13)
        h1 *= -0x3d4d51cb.toInt()
        h1 = h1 xor (h1 ushr 16)

        return h1
    }

    fun insert(phoneNumber: String) {
        val bytes = phoneNumber.toByteArray(Charsets.UTF_8)
        synchronized(lock) {
            for (i in 0 until numHashFunctions) {
                val hash = murmurHash3(bytes, i)
                // Fix: Math.abs(hash % numBits) returns Long on some JVMs — cast to Int for BitSet
                val index = (Math.abs(hash.toLong() % numBits)).toInt()
                bitSet.set(index)
            }
        }
    }

    fun mightContain(phoneNumber: String): Boolean {
        val bytes = phoneNumber.toByteArray(Charsets.UTF_8)
        synchronized(lock) {
            for (i in 0 until numHashFunctions) {
                val hash = murmurHash3(bytes, i)
                val index = (Math.abs(hash.toLong() % numBits)).toInt()
                if (!bitSet.get(index)) {
                    return false
                }
            }
        }
        return true
    }

    fun clear() {
        synchronized(lock) {
            bitSet.clear()
        }
    }
}
