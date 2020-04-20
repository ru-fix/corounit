package ru.fix.corounit.engine

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

open class TestClassState {
    private val beforeEach = ConcurrentLinkedDeque<Int>()
    private val afterEach = ConcurrentLinkedDeque<Int>()
    private val beforeAll = ConcurrentLinkedDeque<Int>()
    private val afterAll = ConcurrentLinkedDeque<Int>()

    private val methodSequences = ConcurrentLinkedDeque<Int>()
    private val methodIds = ConcurrentLinkedDeque<Int>()
    private val counter = AtomicInteger()

    open fun reset() {
        counter.set(0)
        beforeEach.clear()
        afterEach.clear()
        methodSequences.clear()
        methodIds.clear()
        beforeAll.clear()
        afterAll.clear()
    }

    protected fun beforeEachInvoked() {
        beforeEach.addLast(counter.incrementAndGet())
    }

    protected fun beforeAllInvoked() {
        beforeAll.addLast(counter.incrementAndGet())
    }

    protected fun testMethodInvoked(id: Int): Int {
        val count = counter.incrementAndGet()
        methodSequences.addLast(count)
        methodIds.addLast(id)
        return count
    }

    protected fun afterEachInvoked() {
        afterEach.addLast(counter.incrementAndGet())
    }

    protected fun afterAllInvoked() {
        afterAll.addLast(counter.incrementAndGet())
    }

    val beforeEachState get() = beforeEach.toList()
    val beforeAllState get() = beforeAll.toList()
    val methodSequencesState get() = methodSequences.toList()
    val methodIdsState get() = methodIds.toList()
    val afterEachState get() = afterEach.toList()
    val afterAllState get() = afterAll.toList()
}