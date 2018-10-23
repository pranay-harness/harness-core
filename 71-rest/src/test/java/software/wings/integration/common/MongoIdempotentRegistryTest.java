package software.wings.integration.common;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.Idempotent.SUCCEEDED;
import static software.wings.beans.Idempotent.TENTATIVE;

import com.google.inject.Inject;

import com.mongodb.MongoCommandException;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentRegistry;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.harness.rule.RealMongo;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.threading.Concurrent;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Idempotent;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.WingsPersistence;

import java.security.SecureRandom;
import java.util.ArrayList;

@RealMongo
public class MongoIdempotentRegistryTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject MongoIdempotentRegistry<String> idempotentRegistry;

  IdempotentId id = new IdempotentId("foo");
  IdempotentId dataId = new IdempotentId("data");

  public void concurrencyTest(IdempotentRegistry idempotentRegistry) {
    final ArrayList<Integer> integers = new ArrayList<>();
    SecureRandom random = new SecureRandom();

    Concurrent.test(1, i -> {
      // We need at least one thread to execute positive scenario, else the test will fail
      if (i == 0 || random.nextBoolean()) {
        try (IdempotentLock idempotent = IdempotentLock.create(id, idempotentRegistry)) {
          if (idempotent.alreadyExecuted()) {
            return;
          }

          integers.add(1);
          idempotent.succeeded("foo");
        } catch (UnableToRegisterIdempotentOperationException e) {
          // do nothing
        }
      } else {
        try (IdempotentLock idempotent = IdempotentLock.create(id, idempotentRegistry)) {
        } catch (UnableToRegisterIdempotentOperationException e) {
          // do nothing
        }
      }
    });

    assertEquals(1, integers.size());
  }

  @Test
  public void testMongoRegisterNewAssumptions() {
    wingsPersistence.delete(Idempotent.class, id.getValue());

    Idempotent previousIdempotent = wingsPersistence.findAndModify(idempotentRegistry.query(id),
        idempotentRegistry.registerUpdateOperation(IdempotentLock.defaultTTL), MongoIdempotentRegistry.registerOptions);

    assertEquals(null, previousIdempotent);
    assertEquals(TENTATIVE, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  public void testMongoRegisterTentativeAssumptions() {
    Idempotent tentativeIdempotent = new Idempotent();
    tentativeIdempotent.setUuid(id.getValue());
    tentativeIdempotent.setState(TENTATIVE);
    wingsPersistence.save(tentativeIdempotent);

    Idempotent previousIdempotent = wingsPersistence.findAndModify(idempotentRegistry.query(id),
        idempotentRegistry.registerUpdateOperation(IdempotentLock.defaultTTL), MongoIdempotentRegistry.registerOptions);

    assertEquals(TENTATIVE, previousIdempotent.getState());
    assertEquals(TENTATIVE, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  public void testMongoRegisterSucceededAssumptions() {
    Idempotent doneIdempotent = new Idempotent();
    doneIdempotent.setUuid(id.getValue());
    doneIdempotent.setState(SUCCEEDED);
    wingsPersistence.save(doneIdempotent);

    assertThatThrownBy(()
                           -> wingsPersistence.findAndModify(idempotentRegistry.query(id),
                               idempotentRegistry.registerUpdateOperation(IdempotentLock.defaultTTL),
                               MongoIdempotentRegistry.registerOptions))
        .isInstanceOf(MongoCommandException.class)
        .hasMessageContaining("E11000 ");

    assertEquals(SUCCEEDED, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  public void testMongoUnregisterMissingAssumptions() {
    wingsPersistence.delete(Idempotent.class, id.getValue());
    wingsPersistence.findAndModify(idempotentRegistry.query(id), idempotentRegistry.unregisterUpdateOperation(),
        MongoIdempotentRegistry.unregisterOptions);
    assertEquals(null, wingsPersistence.get(Idempotent.class, id.getValue()));
  }

  @Test
  public void testMongoUnregisterTentativeAssumptions() {
    Idempotent tentativeIdempotent = new Idempotent();
    tentativeIdempotent.setUuid(id.getValue());
    tentativeIdempotent.setState(TENTATIVE);
    wingsPersistence.save(tentativeIdempotent);

    wingsPersistence.findAndModify(idempotentRegistry.query(id), idempotentRegistry.unregisterUpdateOperation(),
        MongoIdempotentRegistry.unregisterOptions);
    assertEquals(null, wingsPersistence.get(Idempotent.class, id.getValue()));
  }

  @Test
  public void testMongoUnregisterSucceededAssumptions() {
    Idempotent doneIdempotent = new Idempotent();
    doneIdempotent.setUuid(id.getValue());
    doneIdempotent.setState(SUCCEEDED);
    wingsPersistence.save(doneIdempotent);

    wingsPersistence.findAndModify(idempotentRegistry.query(id), idempotentRegistry.unregisterUpdateOperation(),
        MongoIdempotentRegistry.unregisterOptions);

    assertEquals(SUCCEEDED, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  @Repeat(times = 10, successes = 10)
  public void testConcurrency() throws InterruptedException {
    wingsPersistence.delete(Idempotent.class, id.getValue());
    concurrencyTest(idempotentRegistry);
  }

  int index;

  public String operation(IdempotentId id) {
    try (IdempotentLock<String> idempotent = IdempotentLock.create(id, idempotentRegistry)) {
      if (idempotent.alreadyExecuted()) {
        return idempotent.getResult();
      }

      String result = id.getValue() + ": result " + ++index;
      idempotent.succeeded(result);
      return result;
    }
  }

  @Test
  public void testResult() throws InterruptedException, UnableToRegisterIdempotentOperationException {
    wingsPersistence.delete(Idempotent.class, dataId.getValue());
    assertEquals("data: result 1", operation(dataId));
    assertEquals("data: result 1", operation(dataId));
  }

  @Test
  public void testTimeout() throws InterruptedException, UnableToRegisterIdempotentOperationException {
    wingsPersistence.delete(Idempotent.class, id.getValue());
    IdempotentLock<String> idempotentLock = IdempotentLock.create(id, idempotentRegistry);
    assertThatThrownBy(() -> IdempotentLock.create(id, idempotentRegistry, ofMillis(200), ofMillis(100), ofHours(1)));
    idempotentLock.close();
  }
}
