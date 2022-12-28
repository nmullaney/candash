package app.candash.cluster

data class CANSignal(
    val name: String,
    val busId: Int,
    val frameId: Hex,
    val startBit: Int,
    val bitLength: Int,
    val factor: Float,
    val offset: Float,
    val serviceIndex: Int = 0,
    val muxIndex: Int = 0,
    val signed: Boolean = false,
    val sna: Float? = null,
)

data class AugmentedCANSignal(
    val name: String,
    val dependsOnSignals: List<String>,
    val calculation: (carState: CarState) -> Float?
)
