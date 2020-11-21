package io.harness.cdng.manifest.state;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ManifestStepTest extends CategoryTest {
  private final ManifestStep manifestStep = new ManifestStep();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testManifestStepExecuteSync() {
    K8sManifest k8Manifest1 =
        K8sManifest.builder()
            .identifier("specsManifest")
            .storeConfigWrapper(
                StoreConfigWrapper.builder()
                    .storeConfig(GitStore.builder()
                                     .connectorRef(ParameterField.createValueField("connector"))
                                     .paths(ParameterField.createValueField(Collections.singletonList("path1")))
                                     .branch(ParameterField.createValueField("master"))
                                     .gitFetchType(FetchType.BRANCH)
                                     .build())
                    .build())
            .build();
    ManifestConfigWrapper manifestConfig1 =
        ManifestConfig.builder().identifier("specsManifest").manifestAttributes(k8Manifest1).build();

    K8sManifest k8Manifest2 =
        K8sManifest.builder()
            .identifier("spec1")
            .storeConfigWrapper(StoreConfigWrapper.builder()
                                    .storeConfig(GitStore.builder()
                                                     .paths(ParameterField.createValueField(
                                                         Collections.singletonList("override/path1")))
                                                     .connectorRef(ParameterField.createValueField("connector"))
                                                     .branch(ParameterField.createValueField("commitId"))
                                                     .gitFetchType(FetchType.COMMIT)
                                                     .build())
                                    .build())
            .build();

    ManifestConfigWrapper manifestConfig2 =
        ManifestConfig.builder().identifier("spec1").manifestAttributes(k8Manifest2).build();

    ValuesManifest valuesManifest1 =
        ValuesManifest.builder()
            .identifier("valuesManifest1")
            .storeConfigWrapper(StoreConfigWrapper.builder()
                                    .storeConfig(GitStore.builder()
                                                     .paths(ParameterField.createValueField(
                                                         Collections.singletonList("overrides/path1")))
                                                     .connectorRef(ParameterField.createValueField("connector1"))
                                                     .branch(ParameterField.createValueField("commitId1"))
                                                     .gitFetchType(FetchType.COMMIT)
                                                     .build())
                                    .build())
            .build();
    ManifestConfigWrapper manifestConfig3 =
        ManifestConfig.builder().identifier("valuesManifest1").manifestAttributes(valuesManifest1).build();

    K8sManifest k8Manifest3 =
        K8sManifest.builder()
            .identifier("spec1")
            .storeConfigWrapper(StoreConfigWrapper.builder()
                                    .storeConfig(GitStore.builder()
                                                     .paths(ParameterField.createValueField(
                                                         Collections.singletonList("overrides/path2")))
                                                     .connectorRef(ParameterField.createValueField("connector2"))
                                                     .branch(ParameterField.createValueField("commitId2"))
                                                     .gitFetchType(FetchType.COMMIT)
                                                     .build())
                                    .build())
            .build();

    ManifestConfigWrapper manifestConfig4 =
        ManifestConfig.builder().identifier("spec1").manifestAttributes(k8Manifest3).build();

    ValuesManifest valuesManifest2 =
        ValuesManifest.builder()
            .identifier("valuesManifest1")
            .storeConfigWrapper(StoreConfigWrapper.builder()
                                    .storeConfig(GitStore.builder()
                                                     .paths(ParameterField.createValueField(
                                                         Collections.singletonList("overrides/path3")))
                                                     .connectorRef(ParameterField.createValueField("connector3"))
                                                     .branch(ParameterField.createValueField("commitId3"))
                                                     .gitFetchType(FetchType.COMMIT)
                                                     .build())
                                    .build())
            .build();
    ManifestConfigWrapper manifestConfig5 =
        ManifestConfig.builder().identifier("valuesManifest1").manifestAttributes(valuesManifest2).build();

    StepResponse.StepOutcome stepOutcome =
        manifestStep.processManifests(Arrays.asList(manifestConfig1, manifestConfig2),
            Collections.singletonList(manifestConfig4), Arrays.asList(manifestConfig3, manifestConfig5));

    ManifestOutcome manifestOutcome = (ManifestOutcome) stepOutcome.getOutcome();
    assertThat(manifestOutcome.getManifestAttributes()).isNotEmpty();
    assertThat(manifestOutcome.getManifestAttributes().size()).isEqualTo(3);
    assertThat(manifestOutcome.getManifestAttributes()).containsOnly(k8Manifest1, k8Manifest3, valuesManifest2);
  }
}
