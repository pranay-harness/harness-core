/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.infra.steps;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.steps.StepUtils.createStepResponseFromChildResponse;

import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.environment.EnvironmentMapper;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.common.ParameterFieldHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PrincipalTypeProtoToPrincipalTypeMapper;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.rbac.CDNGRbacPermissions;
import io.harness.steps.OutputExpressionConstants;
import io.harness.steps.environment.EnvironmentOutcome;
import io.harness.steps.executable.ChildExecutableWithRbac;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class InfrastructureSectionStep implements ChildExecutableWithRbac<InfraSectionStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.INFRASTRUCTURE_SECTION.getName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private EnvironmentService environmentService;
  @Inject @Named("PRIVILEGED") private AccessControlClient accessControlClient;

  @Override
  public void validateResources(Ambiance ambiance, InfraSectionStepParameters stepParameters) {
    String accountIdentifier = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);
    ExecutionPrincipalInfo executionPrincipalInfo = ambiance.getMetadata().getPrincipalInfo();
    String principal = executionPrincipalInfo.getPrincipal();
    if (EmptyPredicate.isEmpty(principal)) {
      return;
    }
    PrincipalType principalType = PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(
        executionPrincipalInfo.getPrincipalType());

    if (stepParameters.getEnvironmentRef() == null
        || EmptyPredicate.isEmpty(stepParameters.getEnvironmentRef().getValue())) {
      accessControlClient.checkForAccessOrThrow(Principal.of(principalType, principal),
          ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of("ENVIRONMENT", null),
          CDNGRbacPermissions.ENVIRONMENT_CREATE_PERMISSION, "Missing Environment Create Permission");
    } else {
      accessControlClient.checkForAccessOrThrow(Principal.of(principalType, principal),
          ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
          Resource.of("ENVIRONMENT", stepParameters.getEnvironmentRef().getValue()),
          CDNGRbacPermissions.ENVIRONMENT_RUNTIME_PERMISSION,
          String.format(
              "Missing Access Permission for Environment: [%s]", stepParameters.getEnvironmentRef().getValue()));
    }
  }

  @Override
  public Class<InfraSectionStepParameters> getStepParametersClass() {
    return InfraSectionStepParameters.class;
  }

  @Override
  public ChildExecutableResponse obtainChildAfterRbac(
      Ambiance ambiance, InfraSectionStepParameters stepParameters, StepInputPackage inputPackage) {
    log.info("Starting execution for InfraSection Step [{}]", stepParameters);
    EnvironmentOutcome environmentOutcome = processEnvironment(ambiance, stepParameters.getUseFromStage(),
        stepParameters.getEnvironment(), stepParameters.getEnvironmentRef());
    executionSweepingOutputResolver.consume(
        ambiance, OutputExpressionConstants.ENVIRONMENT, environmentOutcome, StepOutcomeGroup.STAGE.name());

    return ChildExecutableResponse.newBuilder().setChildNodeId(stepParameters.getChildNodeID()).build();
  }

  @Override
  public StepResponse handleChildResponse(
      Ambiance ambiance, InfraSectionStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    log.info("Completed execution for InfraSection Step [{}]", stepParameters);
    return createStepResponseFromChildResponse(responseDataMap);
  }

  EnvironmentOutcome processEnvironment(Ambiance ambiance, InfraUseFromStage useFromStage, EnvironmentYaml environment,
      ParameterField<String> environmentRef) {
    EnvironmentYaml environmentOverrides = null;

    if (useFromStage != null && useFromStage.getOverrides() != null) {
      environmentOverrides = useFromStage.getOverrides().getEnvironment();
      if (EmptyPredicate.isEmpty(environmentOverrides.getName())) {
        environmentOverrides.setName(environmentOverrides.getIdentifier());
      }
    }
    return processEnvironment(environmentOverrides, ambiance, environment, environmentRef);
  }

  private EnvironmentOutcome processEnvironment(EnvironmentYaml environmentOverrides, Ambiance ambiance,
      EnvironmentYaml environmentYaml, ParameterField<String> environmentRef) {
    if (environmentYaml == null) {
      environmentYaml = createEnvYamlFromEnvRef(ambiance, environmentRef);
    }
    if (EmptyPredicate.isEmpty(environmentYaml.getName())) {
      environmentYaml.setName(environmentYaml.getIdentifier());
    }
    EnvironmentYaml finalEnvironmentYaml =
        environmentOverrides != null ? environmentYaml.applyOverrides(environmentOverrides) : environmentYaml;
    Environment environment = getEnvironmentObject(finalEnvironmentYaml, ambiance);
    environmentService.upsert(environment);
    return EnvironmentMapper.toOutcome(finalEnvironmentYaml);
  }

  private Environment getEnvironmentObject(EnvironmentYaml environmentYaml, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);

    TagUtils.removeUuidFromTags(environmentYaml.getTags());

    return Environment.builder()
        .name(environmentYaml.getName())
        .accountId(accountId)
        .type(environmentYaml.getType())
        .identifier(environmentYaml.getIdentifier())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .tags(convertToList(environmentYaml.getTags()))
        .description(ParameterFieldHelper.getParameterFieldValueHandleValueNull(environmentYaml.getDescription()))
        .build();
  }

  private EnvironmentYaml createEnvYamlFromEnvRef(Ambiance ambiance, ParameterField<String> environmentRef) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);
    String envIdentifier = environmentRef.getValue();

    Optional<Environment> optionalEnvironment =
        environmentService.get(accountId, orgIdentifier, projectIdentifier, envIdentifier, false);
    if (optionalEnvironment.isPresent()) {
      Environment env = optionalEnvironment.get();
      return EnvironmentYaml.builder()
          .identifier(envIdentifier)
          .name(env.getName())
          .description(env.getDescription() == null ? ParameterField.createValueField("")
                                                    : ParameterField.createValueField(env.getDescription()))
          .type(env.getType())
          .tags(TagMapper.convertToMap(env.getTags()))
          .build();
    }
    throw new InvalidRequestException("Env with identifier " + envIdentifier + " does not exist");
  }
}
