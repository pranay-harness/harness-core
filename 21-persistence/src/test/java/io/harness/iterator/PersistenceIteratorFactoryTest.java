package io.harness.iterator;

import static io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.config.WorkersConfiguration;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rule.OwnerRule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Duration;
import java.util.Random;

public class PersistenceIteratorFactoryTest extends PersistenceTest {
  @Mock WorkersConfiguration workersConfiguration;
  @InjectMocks @Inject PersistenceIteratorFactory persistenceIteratorFactory;

  private MongoPersistenceIteratorBuilder iteratorBuilder;
  private PumpExecutorOptions pumpExecutorOptions;

  @Before
  public void setUp() throws Exception {
    iteratorBuilder = MongoPersistenceIterator.<DummyClass>builder();
    pumpExecutorOptions =
        PumpExecutorOptions.builder().name("test").interval(Duration.ofSeconds(5)).poolSize(1).build();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCreateIterator() {
    // disable setup
    when(workersConfiguration.confirmWorkerIsActive(DummyClass.class)).thenReturn(false);
    Assertions.assertThat(persistenceIteratorFactory.createIterator(DummyClass.class, iteratorBuilder)).isNull();

    // enable setup
    when(workersConfiguration.confirmWorkerIsActive(DummyClass.class)).thenReturn(true);
    Assertions.assertThat(persistenceIteratorFactory.createIterator(DummyClass.class, iteratorBuilder)).isNotNull();
    // enable setup
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testCreatePumpIteratorWithDedicatedThreadPool() {
    // disable setup
    when(workersConfiguration.confirmWorkerIsActive(DummyClass.class)).thenReturn(false);
    Assertions
        .assertThat(persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
            pumpExecutorOptions, DummyClass.class, iteratorBuilder))
        .isNull();

    // enable setup
    when(workersConfiguration.confirmWorkerIsActive(DummyClass.class)).thenReturn(true);
    Assertions
        .assertThat(persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
            pumpExecutorOptions, DummyClass.class, iteratorBuilder))
        .isNotNull();
    // TODO: check if we can verify scheduleAtFixedRate is called
  }
  private static class DummyClass implements PersistentIterable {
    private Random rand = new Random();
    @Override
    public Long obtainNextIteration(String fieldName) {
      return rand.nextLong();
    }
    @Override
    public String getUuid() {
      return "test";
    }
  }
}