package dev.nmullaney.tesladashboard

class CarState(var carData: MutableMap<String, Float> = mutableMapOf()) {

    fun updateValue(name: String, value: Float) {
        carData[name] = value
    }

    fun getValue(name: String) : Number? {
        val value = carData[name]
        return value
    }
    fun containsKey(key: String) : Boolean {
        return carData.containsKey(key)
    }
}