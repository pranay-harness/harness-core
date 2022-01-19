/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.service;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGTestBase;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.licenserestriction.EnforcementValidator;
import io.harness.cdng.service.beans.KubernetesServiceSpec;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceDefinition;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.StageOverridesConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.UUIDGenerator;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDC)
public class ServicePlanCreatorTest extends CDNGTestBase {
  @Mock private EnforcementValidator enforcementValidator;
  @Inject private KryoSerializer kryoSerializer;
  @Inject @InjectMocks ServicePlanCreator servicePlanCreator;

  @Before
  public void before() throws IllegalAccessException {
    FieldUtils.writeField(servicePlanCreator, "enforcementValidator", enforcementValidator, true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testValidateCreatePlanNodeForArtifacts() {
    DockerHubArtifactConfig primaryArtifact =
        DockerHubArtifactConfig.builder().primaryArtifact(true).identifier("ARTIFACT1").build();
    DockerHubArtifactConfig sidecarArtifact =
        DockerHubArtifactConfig.builder().primaryArtifact(false).identifier("ARTIFACT2").build();
    ArtifactListConfig artifactListConfig1 =
        ArtifactListConfig.builder()
            .primary(PrimaryArtifact.builder().spec(primaryArtifact).build())
            .sidecar(SidecarArtifactWrapper.builder()
                         .sidecar(SidecarArtifact.builder().spec(sidecarArtifact).build())
                         .build())
            .build();

    // Case1: having both primary and sidecars artifacts
    ServiceConfig serviceConfig1 =
        ServiceConfig.builder()
            .serviceDefinition(ServiceDefinition.builder()
                                   .serviceSpec(KubernetesServiceSpec.builder().artifacts(artifactListConfig1).build())
                                   .build())
            .build();
    boolean result = servicePlanCreator.validateCreatePlanNodeForArtifacts(serviceConfig1);
    assertThat(result).isEqualTo(true);

    // Case2: having none primary and sidecars artifacts
    ServiceConfig serviceConfig2 =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().artifacts(ArtifactListConfig.builder().build()).build())
                    .build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(serviceConfig2);
    assertThat(result).isEqualTo(false);

    // Case3: having only sidecars artifacts
    ArtifactListConfig artifactListConfig3 =
        ArtifactListConfig.builder()
            .sidecar(SidecarArtifactWrapper.builder()
                         .sidecar(SidecarArtifact.builder().spec(sidecarArtifact).build())
                         .build())
            .build();

    ServiceConfig serviceConfig3 =
        ServiceConfig.builder()
            .serviceDefinition(ServiceDefinition.builder()
                                   .serviceSpec(KubernetesServiceSpec.builder().artifacts(artifactListConfig3).build())
                                   .build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(serviceConfig3);
    assertThat(result).isEqualTo(true);

    // Case4: having only primary artifacts
    ArtifactListConfig artifactListConfig4 =
        ArtifactListConfig.builder().primary(PrimaryArtifact.builder().spec(primaryArtifact).build()).build();

    ServiceConfig serviceConfig4 =
        ServiceConfig.builder()
            .serviceDefinition(ServiceDefinition.builder()
                                   .serviceSpec(KubernetesServiceSpec.builder().artifacts(artifactListConfig4).build())
                                   .build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(serviceConfig4);
    assertThat(result).isEqualTo(true);

    // StageOverride cases

    // Case1: having both primary and sidecars artifacts
    ServiceConfig stageOverrideServiceConfig1 =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().artifacts(ArtifactListConfig.builder().build()).build())
                    .build())
            .stageOverrides(StageOverridesConfig.builder().artifacts(artifactListConfig1).build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(stageOverrideServiceConfig1);
    assertThat(result).isEqualTo(true);

    // Case2: having none primary and sidecars artifacts
    ServiceConfig stageOverrideServiceConfig2 =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().artifacts(ArtifactListConfig.builder().build()).build())
                    .build())
            .stageOverrides(StageOverridesConfig.builder().artifacts(ArtifactListConfig.builder().build()).build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(stageOverrideServiceConfig2);
    assertThat(result).isEqualTo(false);

    // Case3: having only sidecars artifacts
    ServiceConfig stageOverrideServiceConfig3 =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().artifacts(ArtifactListConfig.builder().build()).build())
                    .build())
            .stageOverrides(StageOverridesConfig.builder().artifacts(artifactListConfig3).build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(stageOverrideServiceConfig3);
    assertThat(result).isEqualTo(true);

    // Case4: having only primary artifacts
    ServiceConfig stageOverrideServiceConfig4 =
        ServiceConfig.builder()
            .serviceDefinition(
                ServiceDefinition.builder()
                    .serviceSpec(
                        KubernetesServiceSpec.builder().artifacts(ArtifactListConfig.builder().build()).build())
                    .build())
            .stageOverrides(StageOverridesConfig.builder().artifacts(artifactListConfig4).build())
            .build();
    result = servicePlanCreator.validateCreatePlanNodeForArtifacts(stageOverrideServiceConfig4);
    assertThat(result).isEqualTo(true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testPrepareMetadataForArtifactsPlanCreator() {
    String uuid = UUIDGenerator.generateUuid();
    ServiceConfig serviceConfig = ServiceConfig.builder().build();
    Map<String, ByteString> metadataDependency =
        servicePlanCreator.prepareMetadataForArtifactsPlanCreator(uuid, serviceConfig);
    assertThat(metadataDependency.size()).isEqualTo(2);
    assertThat(metadataDependency.containsKey(YamlTypes.UUID)).isEqualTo(true);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetFieldClass() {
    assertThat(servicePlanCreator.getFieldClass()).isEqualTo(ServiceConfig.class);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetSupportedTypes() {
    Map<String, Set<String>> supportedTypes = servicePlanCreator.getSupportedTypes();
    assertThat(supportedTypes.containsKey(YamlTypes.SERVICE_CONFIG)).isEqualTo(true);
    assertThat(supportedTypes.get(YamlTypes.SERVICE_CONFIG).size()).isEqualTo(1);
    assertThat(supportedTypes.get(YamlTypes.SERVICE_CONFIG).contains(PlanCreatorUtils.ANY_TYPE)).isEqualTo(true);
  }

  private void checksForDependenciesForArtifact(PlanCreationResponse planCreationResponse, String nodeUuid) {
    assertThat(planCreationResponse.getDependencies().getDependenciesMap().containsKey(nodeUuid)).isEqualTo(true);
    assertThat(planCreationResponse.getDependencies().getDependencyMetadataMap().get(nodeUuid).getMetadataMap().size())
        .isEqualTo(2);
    assertThat(
        planCreationResponse.getDependencies().getDependencyMetadataMap().get(nodeUuid).getMetadataMap().containsKey(
            YamlTypes.UUID))
        .isEqualTo(true);
    assertThat(
        planCreationResponse.getDependencies().getDependencyMetadataMap().get(nodeUuid).getMetadataMap().containsKey(
            YamlTypes.SERVICE_CONFIG))
        .isEqualTo(true);
  }
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAddDependenciesForArtifactsWithServiceDefinition() throws IOException {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream("cdng/plan/service_plan_creator_test1.yml");
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField serviceField = YamlUtils.readTree(yaml);

    PlanCreationContext ctx = PlanCreationContext.builder().currentField(serviceField).build();
    ServiceConfig actualServiceConfig = ServiceConfig.builder().build();
    String nodeUuid = servicePlanCreator.addDependenciesForArtifacts(ctx, planCreationResponseMap, actualServiceConfig);
    assertThat(planCreationResponseMap.size()).isEqualTo(1);
    assertThat(planCreationResponseMap.containsKey(nodeUuid)).isEqualTo(true);
    PlanCreationResponse planCreationResponse1 = planCreationResponseMap.get(nodeUuid);
    checksForDependenciesForArtifact(planCreationResponse1, nodeUuid);
    assertThat(planCreationResponse1.getDependencies().getDependenciesMap().get(nodeUuid))
        .isEqualTo("serviceDefinition/spec/artifacts");
    assertThat(planCreationResponse1.getYamlUpdates()).isNull();
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAddDependenciesForArtifactsWithUseFromStageWithoutStageOverride() throws IOException {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    ServiceConfig actualServiceConfig =
        ServiceConfig.builder().useFromStage(ServiceUseFromStage.builder().stage("stage1").build()).build();
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream("cdng/plan/service_plan_creator_test2.yml");
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField serviceField = YamlUtils.readTree(yaml);

    PlanCreationContext ctx = PlanCreationContext.builder().currentField(serviceField).build();
    String nodeUuid = servicePlanCreator.addDependenciesForArtifacts(ctx, planCreationResponseMap, actualServiceConfig);
    assertThat(planCreationResponseMap.size()).isEqualTo(1);
    assertThat(planCreationResponseMap.containsKey(nodeUuid)).isEqualTo(true);
    PlanCreationResponse planCreationResponse1 = planCreationResponseMap.get(nodeUuid);
    checksForDependenciesForArtifact(planCreationResponse1, nodeUuid);
    assertThat(planCreationResponse1.getDependencies().getDependenciesMap().get(nodeUuid))
        .isEqualTo("stageOverrides/artifacts");
    assertThat(planCreationResponse1.getYamlUpdates().getFqnToYamlCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testAddDependenciesForArtifactsWithUseFromStageWithoutArtifacts() throws IOException {
    LinkedHashMap<String, PlanCreationResponse> planCreationResponseMap = new LinkedHashMap<>();

    ServiceConfig actualServiceConfig =
        ServiceConfig.builder().useFromStage(ServiceUseFromStage.builder().stage("stage1").build()).build();
    ClassLoader classLoader = this.getClass().getClassLoader();
    InputStream yamlFile = classLoader.getResourceAsStream("cdng/plan/service_plan_creator_test3.yml");
    assertThat(yamlFile).isNotNull();

    String yaml = new Scanner(yamlFile, "UTF-8").useDelimiter("\\A").next();
    yaml = YamlUtils.injectUuid(yaml);
    YamlField serviceField = YamlUtils.readTree(yaml);

    PlanCreationContext ctx = PlanCreationContext.builder().currentField(serviceField).build();
    String nodeUuid = servicePlanCreator.addDependenciesForArtifacts(ctx, planCreationResponseMap, actualServiceConfig);
    assertThat(planCreationResponseMap.size()).isEqualTo(1);
    assertThat(planCreationResponseMap.containsKey(nodeUuid)).isEqualTo(true);
    PlanCreationResponse planCreationResponse1 = planCreationResponseMap.get(nodeUuid);
    checksForDependenciesForArtifact(planCreationResponse1, nodeUuid);
    assertThat(planCreationResponse1.getDependencies().getDependenciesMap().get(nodeUuid))
        .isEqualTo("stageOverrides/artifacts");

    assertThat(planCreationResponse1.getYamlUpdates().getFqnToYamlCount()).isEqualTo(1);
  }
}