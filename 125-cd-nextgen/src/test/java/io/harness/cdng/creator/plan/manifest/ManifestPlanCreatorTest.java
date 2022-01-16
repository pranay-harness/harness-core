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

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
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
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.harness.serializer.KryoSerializer;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.CDC)
public class ManifestPlanCreatorTest extends CDNGTestBase {
  @Inject
  private KryoSerializer kryoSerializer;
  @Inject @InjectMocks
  ManifestsPlanCreator manifestsPlanCreator;
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

    Map<String, ByteString> metadataDependency = new HashMap<>();
            metadataDependency.put(YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceConfig)));

    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    PlanCreationContext ctx = PlanCreationContext.builder().dependency(dependency).build();
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> manifestsPlanCreator.createPlanForChildrenNodes(ctx,null))
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

    Map<String, ByteString> metadataDependency = new HashMap<>();
    metadataDependency.put(YamlTypes.SERVICE_CONFIG, ByteString.copyFrom(kryoSerializer.asDeflatedBytes(serviceConfig)));

    Dependency dependency = Dependency.newBuilder().putAllMetadata(metadataDependency).build();
    PlanCreationContext ctx = PlanCreationContext.builder().dependency(dependency).build();

    LinkedHashMap<String, PlanCreationResponse> response = manifestsPlanCreator.createPlanForChildrenNodes(ctx, null);

        List<String> manifestIdentifiers = new ArrayList<>();
        for(Map.Entry<String,PlanCreationResponse> entry: response.entrySet()){
          manifestIdentifiers.add(entry.getValue().getPlanNode().getIdentifier());
        }

        assertThat(manifestIdentifiers).containsExactly("m1", "m2", "m3", "m4", "m5", "m6");
  }

  private ManifestConfigWrapper manifestWith(String identifier, ManifestConfigType type) {
    return ManifestConfigWrapper.builder()
        .manifest(ManifestConfig.builder().identifier(identifier).type(type).build())
        .build();
  }
}
