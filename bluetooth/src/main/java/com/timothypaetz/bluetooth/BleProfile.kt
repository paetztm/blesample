package com.timothypaetz.bluetooth

import java.util.UUID

data class BleProfile(
    val name: String,
    val services: List<BleService>,
)

data class BleService(
    val uuid: UUID,
    val name: String,
    val characteristics: List<BleCharacteristic>,
)

data class BleCharacteristic(
    val uuid: UUID,
    val name: String,
    val timeoutInMillis: Long,
)