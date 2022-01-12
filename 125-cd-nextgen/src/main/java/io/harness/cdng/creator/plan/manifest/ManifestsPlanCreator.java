/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.manifest.steps.ManifestStep;
import io.harness.cdng.manifest.steps.ManifestStepParameters;
import io.harness.cdng.manifest.steps.ManifestStepParameters.ManifestStepParametersBuilder;
import io.harness.cdng.manifest.steps.ManifestsStep;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.fork.ForkStepParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class ManifestsPlanCreator {
  public PlanCreationResponse createPlanForManifestsNode(ServiceConfig serviceConfig, String finalManifestsId) {
    List<ManifestConfigWrapper> manifestListConfig =
        serviceConfig.getServiceDefinition().getServiceSpec().getManifests();
    ManifestListBuilder manifestListBuilder = new ManifestListBuilder(manifestListConfig);
    manifestListBuilder.addOverrideSets(serviceConfig);
    manifestListBuilder.addStageOverrides(serviceConfig);
    ManifestList manifestList = manifestListBuilder.build();
    if (EmptyPredicate.isEmpty(manifestList.getManifests())) {
      return PlanCreationResponse.builder().build();
    }

    List<PlanNode> planNodes = new ArrayList<>();
    for (Map.Entry<String, ManifestInfo> entry : manifestList.getManifests().entrySet()) {
      planNodes.add(createPlanForManifestNode(entry.getKey(), entry.getValue()));
    }

    ForkStepParameters stepParameters =
        ForkStepParameters.builder()
            .parallelNodeIds(planNodes.stream().map(PlanNode::getUuid).collect(Collectors.toList()))
            .build();
    PlanNode manifestsNode =
        PlanNode.builder()
            .uuid(finalManifestsId)
            .stepType(ManifestsStep.STEP_TYPE)
            .name(PlanCreatorConstants.MANIFESTS_NODE_NAME)
            .identifier(YamlTypes.MANIFEST_LIST_CONFIG)
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILDREN).build())
                    .build())
            .skipExpressionChain(false)
            .build();
    planNodes.add(manifestsNode);
    return PlanCreationResponse.builder()
        .nodes(planNodes.stream().collect(Collectors.toMap(PlanNode::getUuid, Function.identity())))
        .build();
  }

  private PlanNode createPlanForManifestNode(String identifier, ManifestInfo manifestInfo) {
    return PlanNode.builder()
        .uuid(UUIDGenerator.generateUuid())
        .stepType(ManifestStep.STEP_TYPE)
        .name(PlanCreatorConstants.MANIFEST_NODE_NAME)
        .identifier(identifier)
        .stepParameters(manifestInfo.getParams())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                .build())
        .skipExpressionChain(false)
        .build();
  }

  @Value
  private static class ManifestList {
    Map<String, ManifestInfo> manifests;
  }

  @Data
  @FieldDefaults(level = AccessLevel.PRIVATE)
  private static class ManifestListBuilder {
    Map<String, ManifestInfoBuilder> manifests;

    ManifestListBuilder(List<ManifestConfigWrapper> manifestConfigList) {
      if (manifestConfigList == null) {
        this.manifests = null;
        return;
      }

      this.manifests = new HashMap<>();
      if (EmptyPredicate.isNotEmpty(manifestConfigList)) {
        manifestConfigList.forEach(mcw -> {
          ManifestConfig mc = mcw.getManifest();
          if (manifests.containsKey(mc.getIdentifier())) {
            throw new InvalidRequestException(
                String.format("Duplicate identifier: [%s] in manifests", mc.getIdentifier()));
          }
          this.manifests.put(mc.getIdentifier(),
              new ManifestInfoBuilder(ManifestStepParameters.builder()
                                          .identifier(mc.getIdentifier())
                                          .type(mc.getType().getDisplayName())
                                          .spec(mc.getSpec())
                                          .order(this.manifests.size())));
        });
      }
    }

    ManifestList build() {
      return new ManifestList(manifests == null
              ? null
              : manifests.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build())));
    }

    void addOverrideSets(ServiceConfig serviceConfig) {
      if (serviceConfig.getStageOverrides() == null
          || ParameterField.isNull(serviceConfig.getStageOverrides().getUseManifestOverrideSets())) {
        return;
      }

      for (String useManifestOverrideSet : serviceConfig.getStageOverrides().getUseManifestOverrideSets().getValue()) {
        List<ManifestOverrideSets> manifestOverrideSetsList =
            serviceConfig.getServiceDefinition()
                .getServiceSpec()
                .getManifestOverrideSets()
                .stream()
                .map(ManifestOverrideSetWrapper::getOverrideSet)
                .filter(overrideSet -> overrideSet.getIdentifier().equals(useManifestOverrideSet))
                .collect(Collectors.toList());
        if (manifestOverrideSetsList.size() == 0) {
          throw new InvalidRequestException(
              String.format("Invalid identifier [%s] in manifest override sets", useManifestOverrideSet));
        }
        if (manifestOverrideSetsList.size() > 1) {
          throw new InvalidRequestException(
              String.format("Duplicate identifier [%s] in manifest override sets", useManifestOverrideSet));
        }

        List<ManifestConfigWrapper> manifestConfigList = manifestOverrideSetsList.get(0).getManifests();
        addOverrides(manifestConfigList, ManifestStepParametersBuilder::overrideSet);
      }
    }

    void addStageOverrides(ServiceConfig serviceConfig) {
      if (serviceConfig.getStageOverrides() == null || serviceConfig.getStageOverrides().getManifests() == null) {
        return;
      }
      List<ManifestConfigWrapper> manifestConfigList = serviceConfig.getStageOverrides().getManifests();
      addOverrides(manifestConfigList, ManifestStepParametersBuilder::stageOverride);
    }

    private void addOverrides(List<ManifestConfigWrapper> manifestConfigList,
        BiConsumer<ManifestStepParametersBuilder, ManifestAttributes> consumer) {
      if (EmptyPredicate.isEmpty(manifestConfigList)) {
        return;
      }

      for (ManifestConfigWrapper manifestConfigWrapper : manifestConfigList) {
        ManifestConfig mc = manifestConfigWrapper.getManifest();
        ManifestInfoBuilder manifestInfoBuilder = manifests.computeIfAbsent(mc.getIdentifier(),
            identifier
            -> new ManifestInfoBuilder(
                ManifestStepParameters.builder().order(manifests.size()).identifier(identifier)));
        consumer.accept(manifestInfoBuilder.getBuilder(), mc.getSpec());
      }
    }
  }

  @Value
  private static class ManifestInfo {
    ManifestStepParameters params;
  }

  @Value
  private static class ManifestInfoBuilder {
    ManifestStepParametersBuilder builder;

    ManifestInfo build() {
      return new ManifestInfo(builder.build());
    }
  }
}
