package io.harness.state.core.section;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SectionStateProducerTest extends OrchestrationBeansTest {
  @Inject private SectionStateProducer sectionStateProducer;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void produce() {
    assertThat(sectionStateProducer.produce()).isNotNull();
    assertThat(sectionStateProducer.produce()).isInstanceOf(SectionState.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void getType() {
    assertThat(sectionStateProducer.getType()).isNotNull();
    assertThat(sectionStateProducer.getType().getType()).isEqualTo("SECTION");
  }
}