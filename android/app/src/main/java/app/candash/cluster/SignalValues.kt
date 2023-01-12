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

    const val rearFogSwitchOff = 0f
    const val rearFogSwitchOn = 1f

    const val lightsOff = 0f
    const val lightsPos = 1f
    const val lightDRL = 2f
    const val lightDRLPlusPos = 3f
    const val lightsOn = 4f

    const val lightSwitchAuto = 0f
    const val lightSwitchOn = 1f
    const val lightSwitchPos = 2f
    const val lightSwitchOff = 3f

    const val highBeamIdle = 0f
    const val highBeamInt = 1f
    const val highBeamLatched = 2f
    const val highBeamAuto = 3f

    const val brakeParkOff = 0f
    const val brakeParkRed = 1f
    const val brakeParkAmber = 2f
}