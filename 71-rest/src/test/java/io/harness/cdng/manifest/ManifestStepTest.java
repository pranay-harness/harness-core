package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.state.ManifestStepParameters;
import io.harness.cdng.manifest.yaml.FetchType;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.execution.status.Status;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;

public class ManifestStepTest extends CategoryTest {
  private final ManifestStep manifestStep = new ManifestStep();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testManifestStepExecuteSync() {
    K8sManifest k8Manifest1 = K8sManifest.builder()
                                  .identifier("specsManifest")
                                  .storeConfig(GitStore.builder()
                                                   .connectorId("connector")
                                                   .path("path1")
                                                   .fetchValue("master")
                                                   .fetchType(FetchType.BRANCH)
                                                   .build())
                                  .build();
    ManifestConfigWrapper manifestConfig1 =
        ManifestConfig.builder().identifier("specsManifest").manifestAttributes(k8Manifest1).build();

    K8sManifest k8Manifest2 = K8sManifest.builder()
                                  .identifier("spec1")

                                  .storeConfig(GitStore.builder()
                                                   .path("override/path1")
                                                   .connectorId("connector")
                                                   .fetchValue("commitId")
                                                   .fetchType(FetchType.COMMIT)
                                                   .build())
                                  .build();

    ManifestConfigWrapper manifestConfig2 =
        ManifestConfig.builder().identifier("spec1").manifestAttributes(k8Manifest2).build();

    ValuesManifest valuesManifest1 = ValuesManifest.builder()
                                         .identifier("valuesManifest1")
                                         .storeConfig(GitStore.builder()
                                                          .path("overrides/path1")
                                                          .connectorId("connector1")
                                                          .fetchValue("commitId1")
                                                          .fetchType(FetchType.COMMIT)
                                                          .build())
                                         .build();
    ManifestConfigWrapper manifestConfig3 =
        ManifestConfig.builder().identifier("valuesManifest1").manifestAttributes(valuesManifest1).build();

    K8sManifest k8Manifest3 = K8sManifest.builder()
                                  .identifier("spec1")
                                  .storeConfig(GitStore.builder()
                                                   .path("overrides/path2")
                                                   .connectorId("connector2")
                                                   .fetchValue("commitId2")
                                                   .fetchType(FetchType.COMMIT)
                                                   .build())
                                  .build();

    ManifestConfigWrapper manifestConfig4 =
        ManifestConfig.builder().identifier("spec1").manifestAttributes(k8Manifest3).build();

    ValuesManifest valuesManifest2 = ValuesManifest.builder()
                                         .identifier("valuesManifest1")
                                         .storeConfig(GitStore.builder()
                                                          .path("overrides/path3")
                                                          .connectorId("connector3")
                                                          .fetchValue("commitId3")
                                                          .fetchType(FetchType.COMMIT)
                                                          .build())
                                         .build();
    ManifestConfigWrapper manifestConfig5 =
        ManifestConfig.builder().identifier("valuesManifest1").manifestAttributes(valuesManifest2).build();

    ManifestStepParameters manifestStepParameters =
        ManifestStepParameters.builder()
            .serviceSpecManifests(Arrays.asList(manifestConfig1, manifestConfig2))
            .manifestOverrideSets(Collections.singletonList(manifestConfig4))
            .stageOverrideManifests(Arrays.asList(manifestConfig3, manifestConfig5))
            .build();

    StepResponse stepResponse = manifestStep.executeSync(null, manifestStepParameters, null, null);

    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes().size()).isEqualTo(1);

    StepResponse.StepOutcome stepOutcome = stepResponse.getStepOutcomes().iterator().next();
    ManifestOutcome manifestOutcome = (ManifestOutcome) stepOutcome.getOutcome();

    assertThat(manifestOutcome.getManifestAttributes()).isNotEmpty();
    assertThat(manifestOutcome.getManifestAttributes().size()).isEqualTo(3);
    assertThat(manifestOutcome.getManifestAttributes()).containsOnly(k8Manifest1, k8Manifest3, valuesManifest2);
  }
}
