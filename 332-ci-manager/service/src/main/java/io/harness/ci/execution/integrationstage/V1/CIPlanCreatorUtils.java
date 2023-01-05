/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.integrationstage.V1;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.yaml.extended.ci.codebase.Build.builder;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.build.BuildStatusUpdateParameter;
import io.harness.beans.build.BuildStatusUpdateParameter.BuildStatusUpdateParameterBuilder;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.yaml.extended.clone.Clone;
import io.harness.beans.yaml.extended.infrastrucutre.DockerInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.HostedVmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.VmInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.VmPoolYaml;
import io.harness.beans.yaml.extended.infrastrucutre.VmPoolYaml.VmPoolYamlSpec;
import io.harness.beans.yaml.extended.platform.Platform;
import io.harness.beans.yaml.extended.platform.V1.PlatformV1;
import io.harness.beans.yaml.extended.runtime.V1.RuntimeV1;
import io.harness.beans.yaml.extended.runtime.V1.VMRuntimeV1;
import io.harness.ci.buildstate.ConnectorUtils;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.ci.states.codebase.ScmGitRefManager;
import io.harness.ci.utils.RepositoryUtils;
import io.harness.cimanager.stages.V1.IntegrationStageNodeV1;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.ng.core.BaseNGAccess;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepGroupElementConfig;
import io.harness.pms.contracts.plan.Dependency;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.PipelineStoreType;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.triggers.TriggerPayload;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.utils.IdentifierGeneratorUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.serializer.KryoSerializer;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.Build.BuildBuilder;
import io.harness.yaml.extended.ci.codebase.BuildType;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.codebase.CodeBase.CodeBaseBuilder;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.PRBuildSpec;
import io.harness.yaml.extended.ci.codebase.impl.TagBuildSpec;
import io.harness.yaml.repository.Reference;
import io.harness.yaml.repository.Repository;
import io.harness.yaml.utils.JsonPipelineUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CI)
@Slf4j
public class CIPlanCreatorUtils {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private ScmGitRefManager scmGitRefManager;

  public Optional<CodeBase> getCodebase(PlanCreationContext ctx, Clone clone) {
    Optional<Repository> optionalRepository = RepositoryUtils.getRepositoryFromPipelineYaml(ctx.getYaml());
    if ((optionalRepository.orElse(null) != null && optionalRepository.get().getDisabled())
        || clone.getDisabled().getValue()) {
      return Optional.empty();
    }
    PipelineStoreType pipelineStoreType = ctx.getPipelineStoreType();
    if (pipelineStoreType == PipelineStoreType.INLINE && optionalRepository.isEmpty()) {
      throw new InvalidRequestException("Repository cannot be empty for inline pipeline if clone is enabled");
    }
    BaseNGAccess ngAccess = BaseNGAccess.builder()
                                .accountIdentifier(ctx.getAccountIdentifier())
                                .orgIdentifier(ctx.getOrgIdentifier())
                                .projectIdentifier(ctx.getProjectIdentifier())
                                .build();
    Repository repository = optionalRepository.orElse(Repository.builder().build());
    CodeBaseBuilder codeBaseBuilder =
        CodeBase.builder()
            .uuid(clone.getUuid())
            .depth(ParameterField.createValueField(repository.getDepth()))
            .sslVerify(ParameterField.createValueField(repository.isInsecure()))
            .prCloneStrategy(ParameterField.createValueField(repository.getStrategy().toPRCloneStrategy()));
    switch (pipelineStoreType) {
      case REMOTE:
        codeBaseBuilder = buildCodebaseForRemotePipeline(ngAccess, ctx, repository, codeBaseBuilder);
        break;
      case INLINE:
        codeBaseBuilder = buildCodebaseForInlinePipeline(ngAccess, repository, codeBaseBuilder);
        break;
      default:
        throw new InvalidRequestException("Invalid Pipeline Store Type : " + pipelineStoreType);
    }
    return Optional.of(codeBaseBuilder.build());
  }

  public Infrastructure getInfrastructure(RuntimeV1 runtime, PlatformV1 platformV1) {
    Platform platform = platformV1.toPlatform();
    switch (runtime.getType()) {
      case CLOUD:
        return HostedVmInfraYaml.builder()
            .spec(HostedVmInfraYaml.HostedVmInfraSpec.builder()
                      .platform(ParameterField.createValueField(platform))
                      .build())
            .build();
      case MACHINE:
        return DockerInfraYaml.builder()
            .spec(DockerInfraYaml.DockerInfraSpec.builder().platform(ParameterField.createValueField(platform)).build())
            .build();
      case VM:
        VMRuntimeV1 vmRuntime = (VMRuntimeV1) runtime;
        return VmInfraYaml.builder()
            .spec(VmPoolYaml.builder()
                      .spec(VmPoolYamlSpec.builder().poolName(vmRuntime.getSpec().getPool()).build())
                      .build())
            .build();
      default:
        throw new InvalidRequestException("Invalid Runtime - " + runtime.getType());
    }
  }

  public Optional<Object> getDeserializedObjectFromDependency(Dependency dependency, String key) {
    if (dependency == null || EmptyPredicate.isEmpty(dependency.getMetadataMap())
        || !dependency.getMetadataMap().containsKey(key)) {
      return Optional.empty();
    }
    byte[] bytes = dependency.getMetadataMap().get(key).toByteArray();
    return EmptyPredicate.isEmpty(bytes) ? Optional.empty() : Optional.of(kryoSerializer.asObject(bytes));
  }

  public ExecutionSource buildExecutionSource(PlanCreationContext ctx, CodeBase codeBase, String identifier) {
    if (codeBase == null) {
      return null;
    }
    PlanCreationContextValue planCreationContextValue = ctx.getGlobalContext().get("metadata");
    ExecutionTriggerInfo triggerInfo = planCreationContextValue.getMetadata().getTriggerInfo();
    TriggerPayload triggerPayload = planCreationContextValue.getTriggerPayload();
    return IntegrationStageUtils.buildExecutionSource(triggerInfo, triggerPayload, identifier, codeBase.getBuild(),
        codeBase.getConnectorRef().getValue(), connectorUtils, ctx, codeBase);
  }

  public BuildStatusUpdateParameter getBuildStatusUpdateParameter(
      IntegrationStageNodeV1 stageNode, CodeBase codebase, ExecutionSource executionSource) {
    if (codebase == null) {
      return null;
    }
    BuildStatusUpdateParameterBuilder builder = BuildStatusUpdateParameter.builder()
                                                    .connectorIdentifier(codebase.getConnectorRef().getValue())
                                                    .repoName(codebase.getRepoName().getValue())
                                                    .name(stageNode.getName())
                                                    .identifier(stageNode.getIdentifier());

    if (executionSource != null && executionSource.getType() == ExecutionSource.Type.WEBHOOK) {
      builder = builder.sha(retrieveLastCommitSha((WebhookExecutionSource) executionSource));
    }
    return builder.build();
  }

  public static List<YamlField> getStepYamlFields(YamlField yamlField) {
    List<YamlNode> yamlNodes = Optional.of(yamlField.getNode().asArray()).orElse(Collections.emptyList());
    return yamlNodes.stream().map(YamlField::new).collect(Collectors.toList());
  }

  public static ExecutionWrapperConfig getExecutionConfig(YamlField step) {
    if (step.getType() == null) {
      throw new InvalidRequestException("Type cannot be null for CI Step");
    }
    switch (step.getType()) {
      case YAMLFieldNameConstants.PARALLEL:
        List<YamlField> parallelNodes = getStepYamlFields(
            step.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YAMLFieldNameConstants.STEPS));
        ParallelStepElementConfig parallelStepElementConfig =
            ParallelStepElementConfig.builder()
                .sections(
                    parallelNodes.stream().map(CIPlanCreatorUtils::getExecutionConfig).collect(Collectors.toList()))
                .build();
        return ExecutionWrapperConfig.builder()
            .uuid(step.getUuid())
            .parallel(getJsonNode(parallelStepElementConfig))
            .build();
      case YAMLFieldNameConstants.GROUP:
        List<YamlField> groupNodes = getStepYamlFields(
            step.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YAMLFieldNameConstants.STEPS));
        StepGroupElementConfig stepGroupElementConfig =
            StepGroupElementConfig.builder()
                .identifier(IdentifierGeneratorUtils.getId(step.getNodeName()))
                .name(step.getNodeName())
                .steps(groupNodes.stream().map(CIPlanCreatorUtils::getExecutionConfig).collect(Collectors.toList()))
                .build();
        return ExecutionWrapperConfig.builder()
            .uuid(step.getUuid())
            .stepGroup(getJsonNode(stepGroupElementConfig))
            .build();
      default:
        return ExecutionWrapperConfig.builder().uuid(step.getUuid()).step(step.getNode().getCurrJsonNode()).build();
    }
  }

  private CodeBaseBuilder buildCodebaseForRemotePipeline(
      BaseNGAccess ngAccess, PlanCreationContext ctx, Repository repository, CodeBaseBuilder builder) {
    GitSyncBranchContext gitSyncBranchContext = deserializeGitSyncBranchContext(ctx.getGitSyncBranchContext());
    if (gitSyncBranchContext == null) {
      throw new InvalidRequestException("Git sync data cannot be null for remote pipeline");
    }
    boolean connectorOverride = !ParameterField.isBlank(repository.getConnector());
    ParameterField<String> repoName = !connectorOverride && ParameterField.isBlank(repository.getName())
        ? ParameterField.createValueField(gitSyncBranchContext.getGitBranchInfo().getRepoName())
        : repository.getName();
    ParameterField<String> connector = ParameterField.isBlank(repository.getConnector())
        ? ParameterField.createValueField(ctx.getMetadata().getMetadata().getPipelineConnectorRef())
        : repository.getConnector();
    return builder
        .build(ParameterField.createValueField(
            getBuildForRemotePipeline(ngAccess, repository, gitSyncBranchContext, connectorOverride)))
        .repoName(repoName)
        .connectorRef(connector);
  }

  private CodeBaseBuilder buildCodebaseForInlinePipeline(
      BaseNGAccess ngAccess, Repository repository, CodeBaseBuilder builder) {
    if (ParameterField.isBlank(repository.getConnector())) {
      throw new InvalidRequestException("Connector should not be empty for inline pipeline");
    }
    return builder.build(ParameterField.createValueField(getBuild(ngAccess, repository)))
        .connectorRef(repository.getConnector())
        .repoName(repository.getName());
  }

  private Build getBuildForRemotePipeline(BaseNGAccess ngAccess, Repository repository,
      GitSyncBranchContext gitSyncBranchContext, boolean connectorOverride) {
    BuildBuilder builder = builder();
    ParameterField<Reference> referenceField = repository.getReference();
    if (!connectorOverride && ParameterField.isNull(referenceField)) {
      return builder.type(BuildType.BRANCH)
          .spec(BranchBuildSpec.builder()
                    .branch(ParameterField.createValueField(gitSyncBranchContext.getGitBranchInfo().getBranch()))
                    .build())
          .build();
    }
    return getBuild(ngAccess, repository);
  }

  private Build getBuild(BaseNGAccess ngAccess, Repository repository) {
    BuildBuilder builder = builder();
    ParameterField<Reference> referenceField = repository.getReference();
    // if reference is null, try to fetch default branch and clone with that

    if (ParameterField.isNull(referenceField)) {
      Optional<String> optionalDefaultBranch = getDefaultBranchIfApplicable(ngAccess, repository);
      if (optionalDefaultBranch.isPresent()) {
        return builder.type(BuildType.BRANCH)
            .spec(
                BranchBuildSpec.builder().branch(ParameterField.createValueField(optionalDefaultBranch.get())).build())
            .build();
      }
    }

    // TODO: to use reference from inputs payload
    Reference reference = referenceField.getValue();
    ParameterField<String> value = ParameterField.createValueField(reference.getValue());
    switch (reference.getType()) {
      case BRANCH:
        builder = builder.type(BuildType.BRANCH).spec(BranchBuildSpec.builder().branch(value).build());
        break;
      case TAG:
        builder = builder.type(BuildType.TAG).spec(TagBuildSpec.builder().tag(value).build());
        break;
      case PR:
        builder = builder.type(BuildType.PR).spec(PRBuildSpec.builder().number(value).build());
        break;
      default:
        throw new InvalidRequestException(String.format("Invalid reference type given: %s", reference.getType()));
    }
    return builder.build();
  }

  private Optional<String> getDefaultBranchIfApplicable(BaseNGAccess ngAccess, Repository repository) {
    if (ParameterField.isNull(repository.getConnector())) {
      return Optional.empty();
    }
    String connectorIdentifier = (String) repository.getConnector().fetchFinalValue();
    String repoName = (String) repository.getName().fetchFinalValue();
    ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, connectorIdentifier);
    try {
      String defaultBranch = scmGitRefManager.getDefaultBranch(
          scmGitRefManager.getScmConnector(connectorDetails, ngAccess.getAccountIdentifier(), repoName),
          connectorIdentifier);
      return Optional.of(defaultBranch);
    } catch (Exception ex) {
      log.error(String.format("Cannot find default branch for connector: %s", connectorIdentifier));
      throw ex;
    }
  }

  private GitSyncBranchContext deserializeGitSyncBranchContext(ByteString byteString) {
    if (isEmpty(byteString)) {
      return null;
    }
    byte[] bytes = byteString.toByteArray();
    return isEmpty(bytes) ? null : (GitSyncBranchContext) kryoSerializer.asInflatedObject(bytes);
  }

  private String retrieveLastCommitSha(WebhookExecutionSource webhookExecutionSource) {
    if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
      PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();
      return prWebhookEvent.getBaseAttributes().getAfter();
    } else if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
      BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();
      return branchWebhookEvent.getBaseAttributes().getAfter();
    }
    log.error("Non supported event type, status will be empty");
    return "";
  }

  private static JsonNode getJsonNode(Object object) {
    try {
      String json = JsonPipelineUtils.writeJsonString(object);
      return JsonPipelineUtils.getMapper().readTree(json);
    } catch (IOException e) {
      throw new CIStageExecutionException("Failed to serialise node", e);
    }
  }
}
