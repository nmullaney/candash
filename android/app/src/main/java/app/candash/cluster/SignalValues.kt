package app.candash.cluster

object SVal {
    const val fusedSpeedNone = 31f

    const val gearInvalid = 0f
    const val gearPark  = 1f
    const val gearReverse = 2f
    const val gearNeutral = 3f
    const val gearDrive = 4f

    const val doorOpen = 1f
    const val doorClosed = 2f

    const val mapUS = 0f
    const val mapEU = 1f
    const val mapNone = 2f
    const val mapCN = 3f
    const val mapAU = 4f
    const val mapJP = 5f
    const val mapTW = 6f
    const val mapKR = 7f
    const val mapME = 8f
    const val mapHK = 9f
    const val mapMO = 10f

    const val awd = 1f
    const val rwd = 0f

    const val chargeStatusActive = 1f
    const val chargeStatusInactive = 0f

    const val lowBeamLeftOff = 0f
    const val lowBeamLeftOn = 1f

    const val frontFogSwitchOff = 0f
    const val frontFogSwitchOn = 1f

    const val rearFogSwitchOff = 0f
    const val rearFogSwitchOn = 1f

    const val drlModeOff = 0f
    const val drlModePosition = 1f
    const val drlModeDrl = 2f

    const val brakeParkOff = 0f
    const val brakeParkRed = 1f
    const val brakeParkAmber = 2f

    const val lightsOff = 3f
    const val lightsPark = 2f
    const val lightsOn = 1f
    const val lightsAuto = 0f
}