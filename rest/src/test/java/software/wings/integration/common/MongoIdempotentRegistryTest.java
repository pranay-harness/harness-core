package software.wings.integration.common;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.Idempotent.SUCCEEDED;
import static software.wings.beans.Idempotent.TENTATIVE;

import com.google.inject.Inject;

import com.mongodb.MongoCommandException;
import io.harness.exception.UnableToRegisterIdempotentOperationException;
import io.harness.idempotence.IdempotentId;
import io.harness.idempotence.IdempotentLock;
import io.harness.idempotence.IdempotentRegistry;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.threading.Concurrent;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Idempotent;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.security.SecureRandom;
import java.util.ArrayList;

@Integration
public class MongoIdempotentRegistryTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject MongoIdempotentRegistry idempotentRegistry;

  IdempotentId id = new IdempotentId("foo");

  public void concurrencyTest(IdempotentRegistry idempotentRegistry) {
    final ArrayList<Integer> integers = new ArrayList<>();
    SecureRandom random = new SecureRandom();

    Concurrent.test(1, i -> {
      // We need at least one thread to execute positive scenario, else the test will fail
      if (i == 0 || random.nextBoolean()) {
        try (IdempotentLock idempotent = IdempotentLock.create(id, idempotentRegistry)) {
          if (idempotent == null) {
            return;
          }
          integers.add(1);
          idempotent.succeeded();
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
        idempotentRegistry.registerUpdateOperation(), MongoIdempotentRegistry.registerOptions);

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
        idempotentRegistry.registerUpdateOperation(), MongoIdempotentRegistry.registerOptions);

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
                               idempotentRegistry.registerUpdateOperation(), MongoIdempotentRegistry.registerOptions))
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
  public void testMongoUnegisterSucceededAssumptions() {
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
}
