package io.harness.cdng.service.steps;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.FetchType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.rule.Owner;
import io.harness.state.io.StepOutcomeRef;
import io.harness.state.io.StepResponseNotifyData;
import org.joor.Reflect;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;

public class ServiceStepTest extends CategoryTest {
  private final ServiceStep serviceStep = new ServiceStep();

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
        .fetchOutcomes(anyList());

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
}
