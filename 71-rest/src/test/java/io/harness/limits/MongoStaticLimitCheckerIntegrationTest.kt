package io.harness.limits

import com.mongodb.DuplicateKeyException
import io.harness.limits.impl.model.StaticLimit
import io.harness.limits.lib.LimitChecker
import io.harness.rule.RepeatRule
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.apache.commons.lang.RandomStringUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import software.wings.dl.WingsPersistence
import software.wings.integration.BaseIntegrationTest
import software.wings.rules.Integration
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail


@Integration
class MongoStaticLimitCheckerIntegrationTest : BaseIntegrationTest() {

    enum class Action {
        CHECK_AND_CONSUME,
        DECREMENT
    }

    data class ActionResult(val action: Action, val value: Boolean)

    @Inject
    private lateinit var dao: WingsPersistence

    var dbSetup = false;

    @Before
    fun setupDb() {
        if (!dbSetup) {
            val ds = dao.datastore
            ds.ensureIndexes(Counter::class.java)
            ds.delete(ds.createQuery(Counter::class.java))
            dbSetup = true
        }
    }

    @After
    fun clearCollection() {
        val ds = dao.datastore
        ds.delete(ds.createQuery(Counter::class.java))
    }

    /**
     * test serialization/deserialization works
     * while writing/fetching from Mongo
     */
    @Test
    fun testSaveAndGetCounter() {
        val ds = dao.datastore

        val initCount = ds.getCount(Counter::class.java)
        val counter = Counter("key", 10)

        val id = dao.save(counter)
        val finalCount = ds.getCount(Counter::class.java)
        assertEquals(initCount + 1, finalCount);

        val counterFromDB = dao.get(Counter::class.java, id)
        assertEquals(counter, counterFromDB)
    }


    @Test
    fun testSaveFailOnDuplicateKey() {
        val counter = Counter("some-key", 10)
        val counter2 = Counter("some-key", 11)

        val passed = try {
            dao.save(counter)
            dao.save(counter2)
            true
        } catch (e: DuplicateKeyException) {
            false
        }

        if (passed) {
            fail("The test should have failed with DuplicateKeyException")
        }
    }

    @Test
    fun testExceptionOnNegativeLimit() {
        var errThrown = false

        try {
            MongoStaticLimitChecker(StaticLimit(-1), dao, "some-identifier")
        } catch (e: IllegalArgumentException) {
            errThrown = true
        }

        assertTrue(errThrown)
    }

    @Test
    fun testCheckAndConsumeSequential() {

        val checker: LimitChecker =
                MongoStaticLimitChecker(StaticLimit(3), dao, "some-identifier")

        assertTrue(checker.checkAndConsume())
        assertTrue(checker.checkAndConsume())
        assertTrue(checker.checkAndConsume())
        assertFalse(checker.checkAndConsume())
    }

    @Test
    fun testWithZeroLimit() {
        val checker =  MongoStaticLimitChecker(StaticLimit(0), dao, "some-identifier")
        assertFalse(checker.checkAndConsume(), "no permits should be given since limit is zero")
        assertFalse(checker.decrement(), "decrement should return false, since nothing to decrement")
        assertFalse(checker.checkAndConsume(), "no permits should be given even after decrement since limit is zero")
    }

    @Test
    @RepeatRule.Repeat(times = 5, successes = 5)
    fun testCheckAndConsumeConcurrent() = runBlocking {
        val key = UUID.randomUUID().toString()
        val limit = 100
        val checker: LimitChecker = MongoStaticLimitChecker(StaticLimit(limit), dao, key)
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
    @RepeatRule.Repeat(times = 5, successes = 5)
    fun testWithDecrementSequential()  {
        val limit = (50..200).shuffled().last()
        val key = UUID.randomUUID().toString()
        val checker: MongoStaticLimitChecker = MongoStaticLimitChecker(StaticLimit(limit), dao, key)

        // concurrent checkAndConsume requests
        dao.save(Counter(key, 0)) // initial value

        val runs = 500
        val actions = List(runs) {
            val randomIndex: Int = (0 until Action.values().size).shuffled().last()
            Action.values()[randomIndex]
        }

        val results = actions.map {
            when (it) {
                Action.CHECK_AND_CONSUME ->
                    ActionResult(it, checker.checkAndConsume())
                Action.DECREMENT ->
                    ActionResult(it, checker.decrement())
            }
        }

        assertEquals(runs, results.size)

        var usedPermits = 0
        for (result in results) {

            when (result.action) {
                Action.CHECK_AND_CONSUME -> {
                    if (usedPermits < limit) {
                        assertTrue(result.value)
                    } else {
                        assertFalse(result.value)
                    }
                    usedPermits = Math.min(usedPermits + 1, limit)
                }

                Action.DECREMENT -> {
                    if (usedPermits > 0) {
                        assertTrue(result.value)
                        usedPermits--
                    }
                }

            }
        }
    }

    @Test
    @RepeatRule.Repeat(times = 4, successes = 4)
    fun testConsumeWithDecrementConcurrent() = runBlocking  {
        val limit = (50..200).shuffled().last()
        val key = "some-key-" + RandomStringUtils.randomAlphanumeric(5);
        val checker: MongoStaticLimitChecker = MongoStaticLimitChecker(StaticLimit(limit), dao, key)

        dao.save(Counter(key, 0)) // initial value

        val runs = 1000
        val actions = List(runs) {
            val randomIndex: Int = (0 until Action.values().size).shuffled().last()
            Action.values()[randomIndex]
        }

        val results = ConcurrentLinkedQueue<ActionResult>()
        val mutex = Mutex()

        // concurrent checkAndConsume requests
        val jobs: List<Deferred<ActionResult>> = actions.map {
            async {
                when (it) {
                    Action.CHECK_AND_CONSUME ->
                        // mutex blocks necessary to ensure 'correct' ordering in `results` collection
                        // 'correct' means the order in which checkAndConsume, or decrement actions return
                        mutex.withLock {
                                val result = ActionResult(it, checker.checkAndConsume())
                                results.add(result)
                                result
                            }
                    Action.DECREMENT ->
                        mutex.withLock {
                            val result = ActionResult(it, checker.decrement())
                            results.add(result)
                            result
                        }
                }
            }
        }

        jobs.forEach { it.await() }

        assertEquals(runs, results.size)

        var usedPermits = 0
        for (result in results) {
            when (result.action) {
                Action.CHECK_AND_CONSUME -> {
                    if (usedPermits < limit) {
                        assertTrue(result.value, "should return true under limit")
                    } else {
                        assertFalse(result.value, "should return false over limit")
                    }
                    usedPermits = Math.min(usedPermits + 1, limit)
                }

                Action.DECREMENT -> {
                    if (usedPermits > 0) {
//                        assertTrue(result.value, "should decrement used count when it's over zero")
                        usedPermits--
                    }
                }

            }
        }
    }

    @Test
    fun testDecrementWhenKeyNotPresentInDB() {
        val limit = (50..200).shuffled().last()
        val key = "some-key-" + RandomStringUtils.random(5)
        val checker: MongoStaticLimitChecker = MongoStaticLimitChecker(StaticLimit(limit), dao, key)


        assertFalse(checker.decrement())
        assertTrue(checker.checkAndConsume())
        assertTrue(checker.decrement())
    }
}
