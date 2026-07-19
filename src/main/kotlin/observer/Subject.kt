package observer

/**
 * The Subject (Observable) side of the Observer pattern. Observers may [subscribe] and
 * [unsubscribe]; [notifyObservers] pushes a new value to everyone currently subscribed.
 */
interface Subject<T> {
    fun subscribe(observer: Observer<T>)
    fun unsubscribe(observer: Observer<T>)
    fun notifyObservers(value: T)
}
