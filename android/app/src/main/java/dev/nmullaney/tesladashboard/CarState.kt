package dev.nmullaney.tesladashboard

class CarState(var carData: MutableMap<String, Number> = mutableMapOf()) {

    fun updateValue(name: String, value: Number) {
        carData[name] = value
    }

    fun getValue(name: String) : Number? {
        return carData[name]
    }
}