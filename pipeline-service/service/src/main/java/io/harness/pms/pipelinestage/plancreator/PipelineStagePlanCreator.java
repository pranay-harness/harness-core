/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.pipelinestage.plancreator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.plancreator.steps.internal.PmsStepPlanCreatorUtils;
import io.harness.plancreator.strategy.StrategyUtils;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.GraphLayoutNode;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.merger.helpers.RuntimeInputFormHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipelinestage.PipelineStageStepParameters;
import io.harness.pms.pipelinestage.helper.PipelineStageHelper;
import io.harness.pms.pipelinestage.step.PipelineStageStep;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.GraphLayoutResponse;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.security.PmsSecurityContextGuardUtils;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.PipelineVersion;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.steps.pipelinestage.PipelineStageConfig;
import io.harness.steps.pipelinestage.PipelineStageNode;
import io.harness.steps.pipelinestage.PipelineStageOutputs;
import io.harness.when.utils.RunInfoUtils;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineStagePlanCreator implements PartialPlanCreator<PipelineStageNode> {
  @Inject private PipelineStageHelper pipelineStageHelper;
  @Inject private PMSPipelineService pmsPipelineService;
  @Inject KryoSerializer kryoSerializer;
  @Override
  public Class<PipelineStageNode> getFieldClass() {
    return PipelineStageNode.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(
        YAMLFieldNameConstants.STAGE, Collections.singleton(StepSpecTypeConstants.PIPELINE_STAGE));
  }

  @Override
  public String getExecutionInputTemplateAndModifyYamlField(YamlField yamlField) {
    return RuntimeInputFormHelper.createExecutionInputFormAndUpdateYamlFieldForStage(
        yamlField.getNode().getParentNode().getCurrJsonNode());
  }

  public PipelineStageStepParameters getStepParameter(
      PipelineStageConfig config, YamlField pipelineInputs, String stageNodeId) {
    return PipelineStageStepParameters.builder()
        .pipeline(config.getPipeline())
        .org(config.getOrg())
        .project(config.getProject())
        .stageNodeId(stageNodeId)
        .inputSetReferences(config.getInputSetReferences())
        .outputs(ParameterField.createValueField(PipelineStageOutputs.getMapOfString(config.getOutputs())))
        .pipelineInputs(pipelineStageHelper.getInputSetYaml(pipelineInputs))
        .build();
  }

  public void setSourcePrincipal(PlanCreationContextValue executionMetadata) {
    Principal principal = PmsSecurityContextGuardUtils.getPrincipal(executionMetadata.getAccountIdentifier(),
        executionMetadata.getMetadata().getPrincipalInfo(),
        executionMetadata.getMetadata().getTriggerInfo().getTriggeredBy());
    SourcePrincipalContextBuilder.setSourcePrincipal(principal);
    SecurityContextBuilder.setContext(principal);
  }
  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, PipelineStageNode stageNode) {
    PipelineStageConfig config = stageNode.getPipelineStageConfig();
    if (config == null) {
      throw new InvalidRequestException("Pipeline Stage Yaml does not contain spec");
    }
    // Principal is added to fetch Git Entity
    setSourcePrincipal(ctx.getMetadata());
    Optional<PipelineEntity> childPipelineEntity = pmsPipelineService.getPipeline(
        ctx.getAccountIdentifier(), config.getOrg(), config.getProject(), config.getPipeline(), false, false);

    if (!childPipelineEntity.isPresent()) {
      throw new InvalidRequestException(String.format("Child pipeline does not exists %s ", config.getPipeline()));
    }

    pipelineStageHelper.validateNestedChainedPipeline(childPipelineEntity.get());
    pipelineStageHelper.validateFailureStrategy(stageNode.getFailureStrategies());

    // TODO: remove this to enable Strategy support for Pipeline Stage
    if (ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.STRATEGY) != null) {
      throw new InvalidRequestException(
          String.format("Strategy is not supported for Pipeline stage %s", stageNode.getIdentifier()));
    }

    Map<String, YamlField> dependenciesNodeMap = new HashMap<>();
    Map<String, ByteString> metadataMap = new HashMap<>();

    // This will be empty till we enable strategy support for Pipeline Stage
    addDependencyForStrategy(ctx, stageNode, dependenciesNodeMap, metadataMap);

    // Here planNodeId is used to support strategy. Same node id will be passed to child execution for navigation to
    // parent execution
    String planNodeId = StrategyUtils.getSwappedPlanNodeId(ctx, stageNode.getUuid());

    PlanNodeBuilder builder =
        PlanNode.builder()
            .uuid(planNodeId)
            .name(stageNode.getName())
            .identifier(stageNode.getIdentifier())
            .group(StepCategory.STAGE.name())
            .stepType(PipelineStageStep.STEP_TYPE)
            .stepParameters(getStepParameter(config,
                ctx.getCurrentField()
                    .getNode()
                    .getField(YAMLFieldNameConstants.SPEC)
                    .getNode()
                    .getField(YAMLFieldNameConstants.INPUTS),
                planNodeId))
            .skipCondition(SkipInfoUtils.getSkipCondition(stageNode.getSkipCondition()))
            .whenCondition(RunInfoUtils.getRunCondition(stageNode.getWhen()))
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.ASYNC).build())
                    .build())
            .adviserObtainments(
                PmsStepPlanCreatorUtils.getAdviserObtainmentFromMetaData(kryoSerializer, ctx.getCurrentField(), true));
    if (!EmptyPredicate.isEmpty(ctx.getExecutionInputTemplate())) {
      builder.executionInputTemplate(ctx.getExecutionInputTemplate());
    }

    // Dependencies is added for strategy node
    return PlanCreationResponse.builder()
        .graphLayoutResponse(getLayoutNodeInfo(ctx, stageNode))
        .planNode(builder.build())
        .dependencies(
            DependenciesUtils.toDependenciesProto(dependenciesNodeMap)
                .toBuilder()
                .putDependencyMetadata(stageNode.getUuid(), Dependency.newBuilder().putAllMetadata(metadataMap).build())
                .build())
        .build();
  }

  private void addDependencyForStrategy(PlanCreationContext ctx, PipelineStageNode stageNode,
      Map<String, YamlField> dependenciesNodeMap, Map<String, ByteString> metadataMap) {
    StrategyUtils.addStrategyFieldDependencyIfPresent(kryoSerializer, ctx, stageNode.getUuid(),
        stageNode.getIdentifier(), stageNode.getName(), dependenciesNodeMap, metadataMap,
        StrategyUtils.getAdviserObtainments(ctx.getCurrentField(), kryoSerializer, false));
  }

  // This is for graph view of strategy execution
  public GraphLayoutResponse getLayoutNodeInfo(PlanCreationContext context, PipelineStageNode config) {
    Map<String, GraphLayoutNode> stageYamlFieldMap = new LinkedHashMap<>();
    YamlField stageYamlField = context.getCurrentField();
    if (StrategyUtils.isWrappedUnderStrategy(context.getCurrentField())) {
      stageYamlFieldMap = StrategyUtils.modifyStageLayoutNodeGraph(stageYamlField);
    }
    return GraphLayoutResponse.builder().layoutNodes(stageYamlFieldMap).build();
  }

  @Override
  public Set<String> getSupportedYamlVersions() {
    return Set.of(PipelineVersion.V0);
  }
}
