package com.signalgate.multipoint.data.models

data class PermissionStatus(
    val permissionName: String,
    val manifestString: String,
    val description: String,
    val isGranted: Boolean,
    val isRequiredForCoreFunction: Boolean
)