package ru.fix.corounit.engine

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

open class TestClassState {
    private val beforeEach = ConcurrentLinkedDeque<Int>()
    private val afterEach = ConcurrentLinkedDeque<Int>()
    private val beforeAll = ConcurrentLinkedDeque<Int>()
    private val afterAll = ConcurrentLinkedDeque<Int>()

    private val testSequences = ConcurrentLinkedDeque<Int>()
    private val testIds = ConcurrentLinkedDeque<Int>()
    private val counter = AtomicInteger()

    open fun reset() {
        counter.set(0)
        beforeEach.clear()
        afterEach.clear()
        testSequences.clear()
        testIds.clear()
        beforeAll.clear()
        afterAll.clear()
    }

    fun beforeEachInvoked() {
        beforeEach.addLast(counter.incrementAndGet())
    }

    fun beforeAllInvoked() {
        beforeAll.addLast(counter.incrementAndGet())
    }

    fun testMethodInvoked(id: Int): Int {
        val count = counter.incrementAndGet()
        testSequences.addLast(count)
        testIds.addLast(id)
        return count
    }

    fun afterEachInvoked() {
        afterEach.addLast(counter.incrementAndGet())
    }

    fun afterAllInvoked() {
        afterAll.addLast(counter.incrementAndGet())
    }

    val beforeEachState get() = beforeEach.toList()
    val beforeAllState get() = beforeAll.toList()
    val testSequencesState get() = testSequences.toList()
    val testIdsState get() = testIds.toList()
    val afterEachState get() = afterEach.toList()
    val afterAllState get() = afterAll.toList()
}