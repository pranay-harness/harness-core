package io.harness.state.inspection;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

public class StateInspectionServiceTest extends OrchestrationTest {
  @Inject private HPersistence persistence;
  @Inject private StateInspectionService stateInspectionService;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldMerge() throws IOException {
    final String uuid = generateUuid();
    stateInspectionService.append(uuid, asList(DummyStateInspectionData.builder().value("dummy value").build()));

    StateInspection stateInspectionResult = stateInspectionService.get(uuid);
    assertThat(((DummyStateInspectionData) stateInspectionResult.getData().get("dummy")).getValue())
        .isEqualTo("dummy value");

    stateInspectionService.append(uuid, asList(DummiesStateInspectionData.builder().value("dummies value").build()));
    stateInspectionResult = stateInspectionService.get(uuid);
    assertThat(((DummiesStateInspectionData) stateInspectionResult.getData().get("dummies")).getValue())
        .isEqualTo("dummies value");
    assertThat(((DummyStateInspectionData) stateInspectionResult.getData().get("dummy")).getValue())
        .isEqualTo("dummy value");
  }
}
