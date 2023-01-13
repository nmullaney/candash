package app.candash.cluster

import androidx.lifecycle.MutableLiveData

typealias CarState = MutableMap<String, Float?>

fun createCarState(carData: MutableMap<String, Float> = mutableMapOf()): CarState {
    return HashMap(carData)
}

typealias LiveCarState = Map<String, MutableLiveData<Float?>>

fun createLiveCarState(): LiveCarState {
    val liveCarState: MutableMap<String, MutableLiveData<Float?>> = mutableMapOf()
    // Create live data for each signal name
    SName.javaClass.declaredFields.forEach { field ->
        if (field.type == String::class.java) {
            val name = field.get(null) as String
            liveCarState[name] = MutableLiveData(null)
        }
    }
    // Make it immutable
    return liveCarState.toMap()
}

fun LiveCarState.clear() {
    this.forEach {
        it.value.postValue(null)
    }
}
