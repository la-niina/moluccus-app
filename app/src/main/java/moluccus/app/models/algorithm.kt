package moluccus.app.models

object algorithm {
    fun <T> shuffleList(list: MutableList<T>) {
        val size = list.size
        for (i in 0 until size) {
            val randomIndex = (0 until size).random()
            val temp = list[i]
            list[i] = list[randomIndex]
            list[randomIndex] = temp
        }
    }
}