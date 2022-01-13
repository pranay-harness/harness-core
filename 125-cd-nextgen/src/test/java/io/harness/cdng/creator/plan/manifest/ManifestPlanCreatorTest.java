/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.manifest;

import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ACASIAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.steps.ManifestStepParameters;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Comparator;
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
        .isThrownBy(() -> ManifestsPlanCreator.createPlanForManifestsNode(serviceConfig, "manifestId"))
        .withMessageContaining("Duplicate identifier: [test] in manifests");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void shouldCreateWithProperOrder() {
    ServiceConfig serviceConfig =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(KubernetesServiceSpec.builder()
                                     .manifests(Arrays.asList(manifestWith("m1", ManifestConfigType.K8_MANIFEST),
                                         manifestWith("m2", ManifestConfigType.VALUES),
                                         manifestWith("m3", ManifestConfigType.VALUES)))
                                     .build())
                    .build())
            .stageOverrides(
                StageOverridesConfig.builder()
                    .manifests(Arrays.asList(manifestWith("m1", ManifestConfigType.K8_MANIFEST),
                        manifestWith("m4", ManifestConfigType.VALUES), manifestWith("m2", ManifestConfigType.VALUES),
                        manifestWith("m5", ManifestConfigType.VALUES), manifestWith("m6", ManifestConfigType.VALUES),
                        manifestWith("m3", ManifestConfigType.VALUES)))
                    .build())
            .build();
    PlanCreationResponse response = ManifestsPlanCreator.createPlanForManifestsNode(serviceConfig, "manifestId");
    assertThat(response.getNodes()
                   .values()
                   .stream()
                   .map(PlanNode::getStepParameters)
                   .filter(ManifestStepParameters.class ::isInstance)
                   .map(ManifestStepParameters.class ::cast)
                   .sorted(Comparator.comparingInt(ManifestStepParameters::getOrder))
                   .map(ManifestStepParameters::getIdentifier))
        .containsExactly("m1", "m2", "m3", "m4", "m5", "m6");
  }

  private ManifestConfigWrapper manifestWith(String identifier, ManifestConfigType type) {
    return ManifestConfigWrapper.builder()
        .manifest(ManifestConfig.builder().identifier(identifier).type(type).build())
        .build();
  }
}
