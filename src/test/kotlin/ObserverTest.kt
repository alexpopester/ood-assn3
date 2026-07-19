import observer.AbstractSubject
import observer.Observer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObserverTest {

    private class TestSubject<T> : AbstractSubject<T>()

    @Test
    fun `subscribe and notify delivers value`() {
        val subject = TestSubject<Int>()
        val received = mutableListOf<Int>()
        subject.subscribe { received.add(it) }
        subject.notifyObservers(42)
        assertEquals(listOf(42), received)
    }

    @Test
    fun `unsubscribe stops delivery`() {
        val subject = TestSubject<Int>()
        val received = mutableListOf<Int>()
        val obs = Observer<Int> { received.add(it) }
        subject.subscribe(obs)
        subject.notifyObservers(1)
        subject.unsubscribe(obs)
        subject.notifyObservers(2)
        assertEquals(listOf(1), received)
    }

    @Test
    fun `duplicate subscribe does not double-deliver`() {
        val subject = TestSubject<Int>()
        val received = mutableListOf<Int>()
        val obs = Observer<Int> { received.add(it) }
        subject.subscribe(obs)
        subject.subscribe(obs)
        subject.notifyObservers(7)
        assertEquals(listOf(7), received)
    }

    @Test
    fun `multiple observers all receive`() {
        val subject = TestSubject<String>()
        val a = mutableListOf<String>()
        val b = mutableListOf<String>()
        subject.subscribe { a.add(it) }
        subject.subscribe { b.add(it) }
        subject.notifyObservers("hello")
        assertEquals(listOf("hello"), a)
        assertEquals(listOf("hello"), b)
    }

    @Test
    fun `notify with no subscribers is a no-op`() {
        val subject = TestSubject<Int>()
        subject.notifyObservers(99) // should not throw
        assertTrue(true)
    }
}
