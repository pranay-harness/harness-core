package io.harness.persistence.converters;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;

public class DurationConverterTest extends PersistenceTest {
  @Inject private HPersistence persistence;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testConverter() {
    DurationTestEntity entity =
        DurationTestEntity.builder().uuid(generateUuid()).testDuration(Duration.ofSeconds(10)).build();
    String id = persistence.insert(entity);
    assertThat(id).isNotNull();
    DurationTestEntity savedEntity = persistence.get(DurationTestEntity.class, id);
    assertThat(savedEntity).isNotNull();
    assertThat(savedEntity.getUuid()).isEqualTo(id);
    assertThat(savedEntity.getTestDuration()).isEqualTo(Duration.ofSeconds(10));
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testConverterForNull() {
    DurationTestEntity entity = DurationTestEntity.builder().uuid(generateUuid()).build();
    String id = persistence.insert(entity);
    assertThat(id).isNotNull();
    DurationTestEntity savedEntity = persistence.get(DurationTestEntity.class, id);
    assertThat(savedEntity).isNotNull();
    assertThat(savedEntity.getUuid()).isEqualTo(id);
    assertThat(savedEntity.getTestDuration()).isNull();
  }
}