package io.harness.cdng.manifest;

import static io.harness.rule.OwnerRule.ADWAIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.state.ManifestListConfig;
import io.harness.cdng.manifest.state.ManifestStep;
import io.harness.cdng.manifest.state.ManifestStepParameters;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.K8Manifest;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.execution.status.Status;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

public class ManifestStepTest extends CategoryTest {
  private ManifestStep manifestStep = new ManifestStep();

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testManifestStepExecuteSync() throws Exception {
    K8Manifest k8Manifest1 = K8Manifest.builder()
                                 .identifier("specsManifest")
                                 .storeConfig(GitStore.builder()
                                                  .connectorId("connector")
                                                  .fetchValue("master")
                                                  .fetchType("branch")
                                                  .paths(Arrays.asList("path1"))
                                                  .build())
                                 .build();
    ManifestConfigWrapper manifestConfig1 =
        ManifestConfig.builder().identifier("specsManifest").manifestAttributes(k8Manifest1).build();

    K8Manifest k8Manifest2 = K8Manifest.builder()
                                 .identifier("valuesManifest")
                                 .storeConfig(GitStore.builder()
                                                  .connectorId("connector")
                                                  .fetchValue("commitId")
                                                  .fetchType("1234")
                                                  .paths(Arrays.asList("override/path1"))
                                                  .build())
                                 .build();

    ManifestConfigWrapper manifestConfig2 =
        ManifestConfig.builder().identifier("valuesManifest").manifestAttributes(k8Manifest2).build();

    K8Manifest k8Manifest3 = K8Manifest.builder()
                                 .identifier("overrideManifest")
                                 .storeConfig(GitStore.builder()
                                                  .connectorId("connector")
                                                  .fetchValue("commitId")
                                                  .fetchType("789")
                                                  .paths(Arrays.asList("overrides/path1"))
                                                  .build())
                                 .build();
    ManifestConfigWrapper manifestConfig3 =
        ManifestConfig.builder().identifier("overrideManifest").manifestAttributes(k8Manifest3).build();

    ManifestStepParameters manifestStepParameters =
        ManifestStepParameters.builder()
            .manifestServiceSpec(
                ManifestListConfig.builder().manifests(Arrays.asList(manifestConfig1, manifestConfig2)).build())
            .manifestStageOverride(ManifestListConfig.builder().manifests(Arrays.asList(manifestConfig3)).build())
            .build();

    StepResponse stepResponse = manifestStep.executeSync(null, manifestStepParameters, null, null);

    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes().size()).isEqualTo(1);

    StepResponse.StepOutcome stepOutcome = stepResponse.getStepOutcomes().iterator().next();
    ManifestOutcome manifestOutcome = (ManifestOutcome) stepOutcome.getOutcome();

    assertThat(manifestOutcome.getManifestAttributesForServiceSpec()).isNotEmpty();
    assertThat(manifestOutcome.getManifestAttributesForServiceSpec().size()).isEqualTo(2);
    assertThat(manifestOutcome.getManifestAttributesForServiceSpec()).containsOnly(k8Manifest1, k8Manifest2);

    assertThat(manifestOutcome.getManifestAttributesForOverride()).isNotEmpty();
    assertThat(manifestOutcome.getManifestAttributesForOverride().size()).isEqualTo(1);
    assertThat(manifestOutcome.getManifestAttributesForOverride()).containsOnly(k8Manifest3);
  }
}
