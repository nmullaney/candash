package dev.nmullaney.tesladashboard

data class CANSignal (val name : String,
                      val busId : Int,
                      val frameId : Hex,
                      val startBit: Int,
                      val bitLength: Int,
                      val factor: Float,
                      val offset: Int,
                      val signed: Boolean? = false)