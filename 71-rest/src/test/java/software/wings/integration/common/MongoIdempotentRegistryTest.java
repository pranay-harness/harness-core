package software.wings.integration.common;

import static io.harness.eraro.mongo.MongoError.DUPLICATE_KEY;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static software.wings.beans.Idempotent.SUCCEEDED;
import static software.wings.beans.Idempotent.TENTATIVE;

import com.google.inject.Inject;

import com.mongodb.MongoCommandException;
import io.harness.category.element.IntegrationTests;
import io.harness.category.element.UnitTests;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentRegistry;
import io.harness.distribution.idempotence.IdempotentResult;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import io.harness.rule.RealMongo;
import io.harness.threading.Concurrent;
import lombok.Builder;
import lombok.Value;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Idempotent;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.WingsPersistence;

import java.security.SecureRandom;
import java.util.ArrayList;

public class MongoIdempotentRegistryTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Value
  @Builder
  private static class TestIdempotentResult implements IdempotentResult {
    private String value;
  }

  @Inject MongoIdempotentRegistry<TestIdempotentResult> idempotentRegistry;

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
          idempotent.succeeded(TestIdempotentResult.builder().value("foo").build());
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
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoRegisterNewAssumptions() {
    wingsPersistence.delete(Idempotent.class, id.getValue());

    Idempotent previousIdempotent = wingsPersistence.findAndModify(idempotentRegistry.query(id),
        idempotentRegistry.registerUpdateOperation(IdempotentLock.defaultTTL), MongoIdempotentRegistry.registerOptions);

    assertEquals(null, previousIdempotent);
    assertEquals(TENTATIVE, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testMongoRegisterTentativeAssumptions() {
    Idempotent tentativeIdempotent = Idempotent.builder().uuid(id.getValue()).state(TENTATIVE).build();
    wingsPersistence.save(tentativeIdempotent);

    Idempotent previousIdempotent = wingsPersistence.findAndModify(idempotentRegistry.query(id),
        idempotentRegistry.registerUpdateOperation(IdempotentLock.defaultTTL), MongoIdempotentRegistry.registerOptions);

    assertEquals(TENTATIVE, previousIdempotent.getState());
    assertEquals(TENTATIVE, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testMongoRegisterSucceededAssumptions() {
    Idempotent doneIdempotent = Idempotent.builder().uuid(id.getValue()).state(SUCCEEDED).build();
    wingsPersistence.save(doneIdempotent);

    assertThatThrownBy(()
                           -> wingsPersistence.findAndModify(idempotentRegistry.query(id),
                               idempotentRegistry.registerUpdateOperation(IdempotentLock.defaultTTL),
                               MongoIdempotentRegistry.registerOptions))
        .isInstanceOf(MongoCommandException.class)
        .hasMessageContaining("E" + DUPLICATE_KEY.getErrorCode() + " ");

    assertEquals(SUCCEEDED, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  @Category(IntegrationTests.class)
  public void testMongoUnregisterMissingAssumptions() {
    wingsPersistence.delete(Idempotent.class, id.getValue());
    wingsPersistence.findAndModify(idempotentRegistry.query(id), idempotentRegistry.unregisterUpdateOperation(),
        MongoIdempotentRegistry.unregisterOptions);
    assertNull(wingsPersistence.get(Idempotent.class, id.getValue()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testMongoUnregisterTentativeAssumptions() {
    Idempotent tentativeIdempotent = Idempotent.builder().uuid(id.getValue()).state(TENTATIVE).build();
    wingsPersistence.save(tentativeIdempotent);

    wingsPersistence.findAndModify(idempotentRegistry.query(id), idempotentRegistry.unregisterUpdateOperation(),
        MongoIdempotentRegistry.unregisterOptions);
    assertNull(wingsPersistence.get(Idempotent.class, id.getValue()));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testMongoUnregisterSucceededAssumptions() {
    Idempotent doneIdempotent = Idempotent.builder().uuid(id.getValue()).state(SUCCEEDED).build();
    wingsPersistence.save(doneIdempotent);

    wingsPersistence.findAndModify(idempotentRegistry.query(id), idempotentRegistry.unregisterUpdateOperation(),
        MongoIdempotentRegistry.unregisterOptions);

    assertEquals(SUCCEEDED, wingsPersistence.get(Idempotent.class, id.getValue()).getState());
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testConcurrency() throws InterruptedException {
    wingsPersistence.delete(Idempotent.class, id.getValue());
    concurrencyTest(idempotentRegistry);
  }

  int index;

  public String operation(IdempotentId id) {
    try (IdempotentLock<TestIdempotentResult> idempotent = IdempotentLock.create(id, idempotentRegistry)) {
      if (idempotent.alreadyExecuted()) {
        return idempotent.getResult().getValue();
      }

      String result = id.getValue() + ": result " + ++index;
      idempotent.succeeded(TestIdempotentResult.builder().value(result).build());
      return result;
    }
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testResult() throws InterruptedException, UnableToRegisterIdempotentOperationException {
    wingsPersistence.delete(Idempotent.class, dataId.getValue());
    assertEquals("data: result 1", operation(dataId));
    assertEquals("data: result 1", operation(dataId));
  }

  @Test
  @Category(UnitTests.class)
  @RealMongo
  public void testTimeout() throws InterruptedException, UnableToRegisterIdempotentOperationException {
    wingsPersistence.delete(Idempotent.class, id.getValue());
    IdempotentLock<TestIdempotentResult> idempotentLock = IdempotentLock.create(id, idempotentRegistry);
    assertThatThrownBy(() -> IdempotentLock.create(id, idempotentRegistry, ofMillis(200), ofMillis(100), ofHours(1)));
    idempotentLock.close();
  }
}
