package observer

/**
 * The Observer side of the Observer pattern. An observer is notified with a value of type [T]
 * whenever the [Subject] it subscribed to changes.
 */
fun interface Observer<T> {
    fun onUpdate(value: T)
}
