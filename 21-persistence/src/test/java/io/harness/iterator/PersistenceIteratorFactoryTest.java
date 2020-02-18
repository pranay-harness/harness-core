package io.harness.iterator;

import static io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import com.codahale.metrics.MetricRegistry;
import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.config.WorkersConfiguration;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.security.SecureRandom;
import java.time.Duration;

public class PersistenceIteratorFactoryTest extends PersistenceTest {
  @Mock WorkersConfiguration workersConfiguration;
  @Mock HarnessMetricRegistry harnessMetricRegistry;
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
  @Owner(developers = YOGESH)
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
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testCreatePumpIteratorWithDedicatedThreadPool() {
    MetricRegistry metricRegistry = mock(MetricRegistry.class);
    when(harnessMetricRegistry.getThreadPoolMetricRegistry()).thenReturn(metricRegistry);

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
    private static final SecureRandom random = new SecureRandom();

    @Override
    public Long obtainNextIteration(String fieldName) {
      return random.nextLong();
    }
    @Override
    public String getUuid() {
      return "test";
    }
  }
}