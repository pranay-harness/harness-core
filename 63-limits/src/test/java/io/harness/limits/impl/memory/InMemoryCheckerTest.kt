package io.harness.limits.impl.memory

import io.harness.limits.impl.model.StaticLimit
import io.harness.rule.RepeatRule
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class InMemoryLimitCheckerTest {

    @Test
    @RepeatRule.Repeat(times = 10, successes = 10)
    fun testCheckAndConsumeConcurrent() = runBlocking {
        val limit = (100..500).shuffled().last()
        val checker = InMemoryLimitChecker(StaticLimit(limit))
        val results = ConcurrentLinkedQueue<Boolean>()

        // concurrent checkAndConsume requests
        val runs = 1000
        val jobs = List(runs) {
            async {
                val allow = checker.checkAndConsume()
                results.add(allow)
            }
        }

        // wait for all async jobs to complete
        jobs.forEach { it.await() }

        assertEquals(runs, results.size, "number of results should be same as runs")

        var allowedCount = 0
        var disallowedCount = 0

        for (result in results) {
            if (result) {
                allowedCount++
            } else {
                disallowedCount++
            }
        }

        assertEquals(limit, allowedCount, "all requests under limit should pass")
        assertEquals(runs - limit, disallowedCount,"all requests beyond limit should fail")
    }

    @Test
    fun testCheckAndConsumeSequential() {
        val limit = 2
        val checker = InMemoryLimitChecker(StaticLimit(limit))

        assertTrue(checker.checkAndConsume())
        assertTrue(checker.checkAndConsume())
        assertFalse(checker.checkAndConsume())
    }
}
