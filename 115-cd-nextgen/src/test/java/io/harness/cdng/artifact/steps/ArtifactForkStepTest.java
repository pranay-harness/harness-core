package io.harness.cdng.artifact.steps;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponseNotifyData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArtifactForkStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock OutcomeService outcomeService;
  @InjectMocks ArtifactForkStep artifactForkStep;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testObtainChildren() {
    Ambiance ambiance = Ambiance.builder().build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ForkStepParameters forkStepParameters =
        ForkStepParameters.builder().parallelNodeId("Node1").parallelNodeId("Node2").build();

    ChildrenExecutableResponse childrenExecutableResponse =
        artifactForkStep.obtainChildren(ambiance, forkStepParameters, stepInputPackage);
    List<String> children = childrenExecutableResponse.getChildren()
                                .stream()
                                .map(ChildrenExecutableResponse.Child::getChildNodeId)
                                .collect(Collectors.toList());
    assertThat(children.size()).isEqualTo(2);
    assertThat(children).containsOnly("Node1", "Node2");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testHandleChildrenResponse() {
    Map<String, ResponseData> responseDataMap = new HashMap<>();

    DockerArtifactOutcome artifactOutcome =
        DockerArtifactOutcome.builder().artifactType(ArtifactUtils.PRIMARY_ARTIFACT).identifier("ARTIFACT1").build();
    DockerArtifactOutcome artifactOutcome2 =
        DockerArtifactOutcome.builder().artifactType(ArtifactUtils.SIDECAR_ARTIFACT).identifier("ARTIFACT2").build();
    doReturn(Arrays.asList(artifactOutcome, artifactOutcome2)).when(outcomeService).fetchOutcomes(any());

    Ambiance ambiance = Ambiance.builder().build();
    ForkStepParameters forkStepParameters = ForkStepParameters.builder().build();

    StepResponseNotifyData stepResponseNotifyData =
        StepResponseNotifyData.builder()
            .stepOutcomesRefs(
                Collections.singletonList(StepOutcomeRef.builder().instanceId("node1").name("node1").build()))
            .status(Status.SUCCEEDED)
            .build();
    responseDataMap.put("KEY", stepResponseNotifyData);

    StepResponse stepResponse = artifactForkStep.handleChildrenResponse(ambiance, forkStepParameters, responseDataMap);
    Collection<StepOutcome> stepOutcomes = stepResponse.getStepOutcomes();
    assertThat(stepOutcomes.size()).isEqualTo(1);
    StepOutcome next = stepOutcomes.iterator().next();
    assertThat(next.getOutcome()).isInstanceOf(ArtifactsOutcome.class);

    ArtifactsOutcome outcome = (ArtifactsOutcome) next.getOutcome();
    assertThat(outcome.getPrimary()).isEqualTo(artifactOutcome);
    assertThat(outcome.getSidecars().size()).isEqualTo(1);
    assertThat(outcome.getSidecars().get("ARTIFACT2")).isEqualTo(artifactOutcome2);
  }
}