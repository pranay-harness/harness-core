package io.harness.cdng.creator.plan.manifest;

import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class ManifestPlanCreatorTest extends CDNGTestBase {
  @Test
  @Owner(developers = ACASIAN)
  @Category(UnitTests.class)
  public void shouldNotAllowDuplicateManifestIdentifiers() {
    ManifestConfigWrapper k8sManifest =
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder().identifier("test").type(ManifestConfigType.K8_MANIFEST).build())
            .build();
    ManifestConfigWrapper valuesManifest =
        ManifestConfigWrapper.builder()
            .manifest(ManifestConfig.builder().identifier("test").type(ManifestConfigType.VALUES).build())
            .build();

    ServiceConfig serviceConfig =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().manifests(Arrays.asList(k8sManifest, valuesManifest)).build())
                    .build())
            .build();

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> ManifestsPlanCreator.createPlanForManifestsNode(serviceConfig))
        .withMessageContaining("Duplicate identifier: [test] in manifests");
  }
}
