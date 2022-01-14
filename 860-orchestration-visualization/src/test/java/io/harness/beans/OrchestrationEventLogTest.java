package io.harness.beans;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.mongo.index.MongoIndex;
import io.harness.rule.Owner;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationEventLogTest extends OrchestrationVisualizationTestBase {
  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestMongoIndexes() {
    List<MongoIndex> mongoIndices = OrchestrationEventLog.mongoIndexes();

    assertThat(mongoIndices.size()).isEqualTo(2);
    assertThat(mongoIndices.stream().map(MongoIndex::getName).collect(Collectors.toSet()).size()).isEqualTo(2);
  }
}