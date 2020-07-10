package io.harness.cdng.service.steps;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.FetchType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceOutcome.ArtifactsOutcome;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.rule.Owner;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepResponseNotifyData;
import org.joor.Reflect;
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

public class ServiceStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock OutcomeService outcomeService;
  @InjectMocks ServiceStep serviceStep;

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateServiceOutcome() {
    K8sManifest k8Manifest = K8sManifest.builder()
                                 .identifier("m1")
                                 .kind(ManifestType.K8Manifest)
                                 .storeConfig(GitStore.builder()
                                                  .path("path")
                                                  .connectorId("g1")
                                                  .fetchType(FetchType.BRANCH)
                                                  .fetchValue("master")
                                                  .build())
                                 .build();

    K8sManifest k8Manifest1 = K8sManifest.builder()
                                  .identifier("m2")

                                  .kind(ManifestType.K8Manifest)
                                  .storeConfig(GitStore.builder()
                                                   .path("path1")
                                                   .connectorId("g1")
                                                   .fetchType(FetchType.BRANCH)
                                                   .fetchValue("master")
                                                   .build())
                                  .build();

    OutcomeService outcomeService = mock(OutcomeService.class);
    doReturn(Collections.singletonList(
                 ManifestOutcome.builder().manifestAttributes(Arrays.asList(k8Manifest, k8Manifest1)).build()))
        .when(outcomeService)
        .fetchOutcomes(any());

    Reflect.on(serviceStep).set("outcomeService", outcomeService);
    ServiceOutcome serviceOutcome = serviceStep.createServiceOutcome(
        ServiceConfig.builder()
            .identifier("s1")
            .displayName("s1")
            .serviceSpec(ServiceSpec.builder().deploymentType("kubernetes").build())
            .build(),
        Collections.singletonList(
            StepResponseNotifyData.builder()
                .stepOutcomesRefs(Collections.singletonList(StepOutcomeRef.builder().instanceId("1").name("1").build()))
                .build()));

    assertThat(serviceOutcome.getManifests()).isNotEmpty();
    assertThat(serviceOutcome.getManifests().size()).isEqualTo(2);
    assertThat(serviceOutcome.getManifests().get(0)).isEqualTo(k8Manifest);
    assertThat(serviceOutcome.getManifests().get(1)).isEqualTo(k8Manifest1);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testObtainChildren() {
    Ambiance ambiance = Ambiance.builder().build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    ServiceStepParameters stepParameters =
        ServiceStepParameters.builder().parallelNodeId("Node1").parallelNodeId("Node2").build();

    ChildrenExecutableResponse childrenExecutableResponse =
        serviceStep.obtainChildren(ambiance, stepParameters, stepInputPackage);
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

    StepResponseNotifyData stepResponseNotifyData =
        StepResponseNotifyData.builder()
            .stepOutcomesRefs(
                Collections.singletonList(StepOutcomeRef.builder().instanceId("node1").name("node1").build()))
            .status(Status.SUCCEEDED)
            .build();
    responseDataMap.put("KEY", stepResponseNotifyData);

    ArtifactsOutcome artifactsOutcome = ArtifactsOutcome.builder().build();
    ManifestOutcome manifestOutcome = ManifestOutcome.builder().manifestAttributes(Collections.emptyList()).build();
    doReturn(Arrays.asList(artifactsOutcome, manifestOutcome)).when(outcomeService).fetchOutcomes(any());

    Ambiance ambiance = Ambiance.builder().build();
    ServiceConfig serviceConfig = ServiceConfig.builder()
                                      .identifier("IDENTIFIER")
                                      .displayName("DISPLAY")
                                      .serviceSpec(ServiceSpec.builder().deploymentType("DEPLOYMENT_TYPE").build())
                                      .build();
    ServiceConfig serviceConfigOverrides =
        ServiceConfig.builder()
            .identifier("IDENTIFIER1")
            .displayName("DISPLAY")
            .description("DESCRIPTION")
            .serviceSpec(ServiceSpec.builder().deploymentType("DEPLOYMENT_TYPE").build())
            .build();
    ServiceStepParameters stepParameters =
        ServiceStepParameters.builder().service(serviceConfig).serviceOverrides(serviceConfigOverrides).build();

    StepResponse stepResponse = serviceStep.handleChildrenResponse(ambiance, stepParameters, responseDataMap);
    Collection<StepOutcome> stepOutcomes = stepResponse.getStepOutcomes();
    assertThat(stepOutcomes.size()).isEqualTo(1);
    StepOutcome next = stepOutcomes.iterator().next();
    assertThat(next.getOutcome()).isInstanceOf(ServiceOutcome.class);

    ServiceOutcome outcome = (ServiceOutcome) next.getOutcome();
    assertThat(outcome.getArtifacts()).isEqualTo(artifactsOutcome);
    assertThat(outcome.getManifests()).isEqualTo(Collections.emptyList());
  }
}
