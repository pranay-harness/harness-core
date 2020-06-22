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

public class ServiceStepTest extends CategoryTest {
  private ServiceStep serviceState = new ServiceStep();

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testCreateServiceOutcome() throws Exception {
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
                                  .identifier("o1")

                                  .kind(ManifestType.K8Manifest)
                                  .storeConfig(GitStore.builder()
                                                   .path("path1")
                                                   .connectorId("g1")
                                                   .fetchType(FetchType.BRANCH)
                                                   .fetchValue("master")
                                                   .build())
                                  .build();

    OutcomeService outcomeService = mock(OutcomeService.class);
    doReturn(Arrays.asList(ManifestOutcome.builder()
                               .manifestAttributesForServiceSpec(Arrays.asList(k8Manifest))
                               .manifestAttributesForOverride(Arrays.asList(k8Manifest1))
                               .build()))
        .when(outcomeService)
        .fetchOutcomes(anyList());

    Reflect.on(serviceState).set("outcomeService", outcomeService);
    ServiceOutcome serviceOutcome = serviceState.createServiceOutcome(
        ServiceConfig.builder()
            .identifier("s1")
            .displayName("s1")
            .serviceSpec(ServiceSpec.builder().deploymentType("kubernetes").build())
            .build(),
        Arrays.asList(StepResponseNotifyData.builder()
                          .stepOutcomesRefs(Arrays.asList(StepOutcomeRef.builder().instanceId("1").name("1").build()))
                          .build()));

    assertThat(serviceOutcome.getManifests()).isNotEmpty();
    assertThat(serviceOutcome.getManifests().size()).isEqualTo(1);
    assertThat(serviceOutcome.getManifests()).containsOnly(k8Manifest);

    assertThat(serviceOutcome.getOverrides().getManifests()).isNotEmpty();
    assertThat(serviceOutcome.getOverrides().getManifests().size()).isEqualTo(1);
    assertThat(serviceOutcome.getOverrides().getManifests()).containsOnly(k8Manifest1);
  }
}
