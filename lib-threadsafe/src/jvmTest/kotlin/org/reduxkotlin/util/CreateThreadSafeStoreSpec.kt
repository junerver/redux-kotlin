package org.reduxkotlin.util

import kotlinx.coroutines.*
import org.reduxkotlin.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.system.measureTimeMillis
import kotlin.test.*

object CreateThreadSafeStoreSpec : Spek({
    describe("createThreadSafeStore") {
        it("multithreaded increments massively") {
            suspend fun massiveRun(action: suspend () -> Unit) {
                val n = 100  // number of coroutines to launch
                val k = 1000 // times an action is repeated by each coroutine
                val time = measureTimeMillis {
                    coroutineScope {
                        // scope for coroutines
                        repeat(n) {
                            launch {
                                repeat(k) { action() }
                            }
                        }
                    }
                }
                println("Completed ${n * k} actions in $time ms")
            }

            val counterContext = newFixedThreadPoolContext(5, "CounterContext")

            //NOTE: changing this to createStore() breaks the tests
            val store = createThreadSafeStore(counterReducer, TestCounterState())
            runBlocking {
                withContext(counterContext) {
                    massiveRun {
                        store.dispatch(Increment())
                    }
                }
                withContext(counterContext) {
                    assertEquals(100000, store.state.counter)
                }
            }
        }
    }
})

class Increment

data class TestCounterState(val counter: Int = 0)

val counterReducer = { state: TestCounterState, action: Any ->
    when (action) {
        is Increment -> state.copy(counter = state.counter + 1)
        else -> state
    }
}