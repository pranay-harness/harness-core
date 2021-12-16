/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.template.TemplateHelper.convertToEntityVariables;
import static software.wings.sm.states.ApprovalState.APPROVAL_STATE_TYPE_VARIABLE;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.CollectionEntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.CommandType;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateMetadata;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.dto.ImportedTemplateDetails;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.StateType;
import software.wings.sm.StepType;
import software.wings.sm.states.ApprovalState.ApprovalStateKeys;
import software.wings.sm.states.ApprovalState.ApprovalStateType;
import software.wings.sm.states.ArtifactCollectionState.ArtifactCollectionStateKeys;
import software.wings.yaml.workflow.StepYaml;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author rktummala on 10/28/17
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
@TargetModule(HarnessModule._955_CG_YAML)
public class StepYamlHandler extends BaseYamlHandler<StepYaml, GraphNode> {
  private static final String SERVICE_NOW_CREATE_UPDATE_PARAMS = "serviceNowCreateUpdateParams";
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject FeatureFlagService featureFlagService;
  @Inject private TemplateService templateService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private StepYamlBuilderFactory stepYamlBuilderFactory;
  @Inject private ApplicationManifestService applicationManifestService;

  private static final String GCB_OPTIONS = "gcbOptions";
  private static final String GCP_CONFIG_ID = "gcpConfigId";
  private static final String GCP_CONFIG_NAME = "gcpConfigName";
  private static final String REPOSITORY_SPEC = "repositorySpec";
  private static final String GIT_CONFIG_ID = "gitConfigId";
  private static final String GIT_CONFIG_NAME = "gitConfigName";
  private static final String JENKINS_ID = "jenkinsConfigId";
  private static final String JENKINS_NAME = "jenkinsConfigName";

  private GraphNode toBean(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    StepYaml stepYaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    String type = stepYaml.getType();

    if (isEmpty(type)) {
      throw new InvalidRequestException("Step type could not be empty");
    }

    if (isEmpty(stepYaml.getName())) {
      throw new InvalidRequestException("Step name is empty for " + type + " step");
    }

    StepYamlBuilder stepYamlBuilder = stepYamlBuilderFactory.getStepYamlBuilderForStepType(StepType.valueOf(type));
    if (stepYamlBuilder != null) {
      stepYamlBuilder.validate(changeContext);
    }

    // template expressions
    List<TemplateExpression> templateExpressions = Lists.newArrayList();
    if (stepYaml.getTemplateExpressions() != null) {
      TemplateExpressionYamlHandler templateExprYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
      templateExpressions =
          stepYaml.getTemplateExpressions()
              .stream()
              .map(templateExpr -> {
                try {
                  ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, templateExpr);
                  return templateExprYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                } catch (HarnessException e) {
                  throw new WingsException(e);
                }
              })
              .collect(toList());
    }

    // properties
    Map<String, Object> outputProperties = new HashMap<>();

    Map<String, Object> yamlProperties = stepYaml.getProperties();
    if (yamlProperties != null) {
      yamlProperties.forEach((name, value) -> {
        if (stepYamlBuilder != null && isNotEmpty(name)) {
          stepYamlBuilder.convertNameToIdForKnownTypes(name, value, outputProperties, appId, accountId, yamlProperties);
        } else {
          convertNameToIdIfKnownType(name, value, outputProperties, appId, accountId, yamlProperties);
        }
      });
    }

    generateKnownProperties(outputProperties, changeContext);

    validateArtifactCollectionStep(appId, stepYaml, outputProperties);

    validateOutputEnvironmentVariables(stepYaml, outputProperties);

    Boolean isRollback = false;
    if (changeContext.getProperties().get(YamlConstants.IS_ROLLBACK) != null) {
      isRollback = (Boolean) changeContext.getProperties().get(YamlConstants.IS_ROLLBACK);
    }

    String templateUuid = null;
    String templateVersion = null;
    String templateUri = stepYaml.getTemplateUri();
    ImportedTemplateDetails importedTemplateDetail = null;
    TemplateMetadata templateMetadata = null;
    if (isNotEmpty(templateUri)) {
      if (isNotEmpty(appId)) {
        templateUuid = templateService.fetchTemplateIdFromUri(accountId, appId, templateUri);
      } else {
        templateUuid = templateService.fetchTemplateIdFromUri(accountId, templateUri);
      }
      templateVersion = templateService.fetchTemplateVersionFromUri(templateUuid, templateUri);
    }

    if (templateUuid != null && templateVersion != null) {
      Template template = templateService.get(templateUuid, templateVersion);
      notNullCheck(String.format("Template with uri %s is not found.", templateUri), template);
      importedTemplateDetail = TemplateHelper.getImportedTemplateDetails(template, templateVersion);
      templateMetadata = template.getTemplateMetadata();

      if (TemplateType.SSH.toString().equals(template.getType())) {
        outputProperties.put("commandType", CommandType.OTHER);
      }
    }

    return GraphNode.builder()
        .name(stepYaml.getName())
        .type(type)
        .templateExpressions(templateExpressions)
        .rollback(isRollback)
        .properties(outputProperties.isEmpty() ? null : outputProperties)
        .templateUuid(templateUuid)
        .templateVersion(templateVersion)
        .templateMetadata(templateMetadata)
        .importedTemplateDetails(importedTemplateDetail)
        .templateVariables(convertToEntityVariables(stepYaml.getTemplateVariables()))
        .build();
  }

  private void validateOutputEnvironmentVariables(StepYaml stepYaml, Map<String, Object> outputProperties) {
    if (StateType.SHELL_SCRIPT.name().equals(stepYaml.getType()) && isNotEmpty(outputProperties)) {
      String outputVars = (String) outputProperties.get("outputVars");
      String secretOutputVars = (String) outputProperties.get("secretOutputVars");

      List<String> outputVarsList = new ArrayList<>();
      List<String> secretOutputVarsList = new ArrayList<>();

      if (isNotEmpty(outputVars)) {
        outputVarsList = Arrays.asList(outputVars.trim().split("\\s*,\\s*"));
        outputVarsList.replaceAll(String::trim);
      }

      if (isNotEmpty(secretOutputVars)) {
        secretOutputVarsList = Arrays.asList(secretOutputVars.split("\\s*,\\s*"));
        secretOutputVarsList.replaceAll(String::trim);
      }
      Set<String> uniqueOutputVarsList = new HashSet<>(outputVarsList);
      Set<String> uniqueSecretOutputVarsList = new HashSet<>(secretOutputVarsList);

      if (uniqueOutputVarsList.size() < outputVarsList.size()) {
        throw new InvalidRequestException("Duplicate output variables in Shell Script");
      }
      if (uniqueSecretOutputVarsList.size() < secretOutputVarsList.size()) {
        throw new InvalidRequestException("Duplicate Secret output variables in Shell Script");
      }

      Set<String> commonVars =
          outputVarsList.stream().distinct().filter(secretOutputVarsList::contains).collect(Collectors.toSet());

      if (isNotEmpty(commonVars)) {
        throw new InvalidRequestException("Variables cannot be both Secret and String");
      }
    }
  }

  private void validateArtifactCollectionStep(String appId, StepYaml stepYaml, Map<String, Object> outputProperties) {
    if (StateType.ARTIFACT_COLLECTION.name().equals(stepYaml.getType()) && isNotEmpty(outputProperties)) {
      String sourceType = (String) outputProperties.get("sourceType");
      if (CollectionEntityType.MANIFEST.name().equals(sourceType)) {
        validateAppManifest(appId, outputProperties);
      } else {
        validateArtifactSource(outputProperties);
      }
    }
  }

  private void validateArtifactSource(Map<String, Object> outputProperties) {
    String artifactStreamId = (String) outputProperties.get("artifactStreamId");
    if (artifactStreamId != null) {
      ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
      notNullCheck("Artifact stream is null for the given id:" + artifactStreamId, artifactStream, USER);
      validateMandatoryFieldsWithParameterizedArtifactStream(outputProperties, artifactStreamId, artifactStream);
    } else {
      List<Map<String, Object>> templateExpressions =
          (List<Map<String, Object>>) outputProperties.get("templateExpressions");
      if (isEmpty(templateExpressions)
          || templateExpressions.stream().noneMatch(templateExpression
              -> ArtifactCollectionStateKeys.artifactStreamId.equals(templateExpression.get("fieldName")))) {
        throw new InvalidRequestException("Artifact source must either have a value or be templatized");
      }
      validateTemplateExpressions(templateExpressions);
    }
  }

  private void validateAppManifest(String appId, Map<String, Object> outputProperties) {
    String appManifestId = (String) outputProperties.get("appManifestId");
    if (appManifestId != null) {
      ApplicationManifest applicationManifest = applicationManifestService.getById(appId, appManifestId);
      notNullCheck("Application Manifest is null for the given id:" + appManifestId, applicationManifest, USER);
    } else {
      List<Map<String, Object>> templateExpressions =
          (List<Map<String, Object>>) outputProperties.get("templateExpressions");
      if (isEmpty(templateExpressions)
          || templateExpressions.stream().noneMatch(templateExpression
              -> ArtifactCollectionStateKeys.appManifestId.equals(templateExpression.get("fieldName")))) {
        throw new InvalidRequestException("Application Manifest must either have a value or be templatized");
      }
      validateTemplateExpressions(templateExpressions);
    }
  }

  private void validateTemplateExpressions(List<Map<String, Object>> templateExpressions) {
    templateExpressions.stream()
        .filter(templateExpression
            -> !ManagerExpressionEvaluator.matchesVariablePattern((String) templateExpression.get("expression")))
        .findAny()
        .ifPresent(templateExpression -> {
          throw new InvalidRequestException("Template variable:[" + templateExpression.get("expression")
              + "] is not valid, should start with ${ and end with }, can have a-z,A-Z,0-9,-_");
        });
  }

  private void validateMandatoryFieldsWithParameterizedArtifactStream(
      Map<String, Object> outputProperties, String artifactStreamId, ArtifactStream artifactStream) {
    if (artifactStream.isArtifactStreamParameterized()) {
      validateRegex(outputProperties, artifactStream);
      validateBuildNo(outputProperties, artifactStream);
      validateRuntimeValues(outputProperties, artifactStreamId, artifactStream);
    }
  }

  private void validateRegex(Map<String, Object> outputProperties, ArtifactStream artifactStream) {
    boolean regex = false;
    if (outputProperties.containsKey("regex")) {
      regex = (boolean) outputProperties.get("regex");
    }
    if (regex) {
      throw new InvalidRequestException(
          format("Regex cannot be set for parameterized artifact source [%s].", artifactStream.getName()), USER);
    }
  }

  private void validateBuildNo(Map<String, Object> outputProperties, ArtifactStream artifactStream) {
    if (StringUtils.isEmpty((String) outputProperties.get("buildNo"))) {
      throw new InvalidRequestException(
          format("Artifact Source [%s] Parameterized. However, buildNo not provided.", artifactStream.getName()), USER);
    }
  }

  private void validateRuntimeValues(
      Map<String, Object> outputProperties, String artifactStreamId, ArtifactStream artifactStream) {
    Map<String, Object> runtimeValues = (Map<String, Object>) outputProperties.get("runtimeValues");
    if (isEmpty(runtimeValues)) {
      throw new InvalidRequestException(
          format("Artifact Source [%s] Parameterized. However, runtime values not provided.", artifactStream.getName()),
          USER);
    }
    List<String> expectedParameters = artifactStreamService.getArtifactStreamParameters(artifactStreamId);
    if (isNotEmpty(expectedParameters)) {
      for (String parameter : expectedParameters) {
        if (runtimeValues.get(parameter) == null) {
          throw new InvalidRequestException(
              format("Artifact Source [%s] Parameterized. However, all runtime values not provided.",
                  artifactStream.getName()),
              USER);
        }
      }
    }
  }

  private void generateKnownProperties(Map<String, Object> outputProperties, ChangeContext<StepYaml> changeContext) {
    String id = generateUuid();

    String phaseStepId = changeContext.getEntityIdMap().get("PHASE_STEP");
    outputProperties.put("id", id);
    outputProperties.put("parentId", phaseStepId);
    outputProperties.put("subWorkflowId", id);
  }

  @Override
  public StepYaml toYaml(GraphNode step, String appId) {
    Map<String, Object> properties = step.getProperties();
    Map<String, Object> outputProperties = new TreeMap<>();

    StepYamlBuilder stepYamlBuilder =
        stepYamlBuilderFactory.getStepYamlBuilderForStepType(StepType.valueOf(step.getType()));

    if (properties != null) {
      properties.forEach((name, value) -> {
        if (!shouldBeIgnored(name)) {
          if (stepYamlBuilder != null) {
            stepYamlBuilder.convertIdToNameForKnownTypes(name, value, outputProperties, appId, properties);
          } else {
            convertIdToNameIfKnownType(name, value, outputProperties, appId, properties);
          }
        }
      });
    }

    // template expressions
    List<TemplateExpression.Yaml> templateExprYamlList = Lists.newArrayList();
    if (step.getTemplateExpressions() != null) {
      TemplateExpressionYamlHandler templateExpressionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
      templateExprYamlList =
          step.getTemplateExpressions()
              .stream()
              .map(templateExpression -> templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(toList());
    }

    String templateUuid = step.getTemplateUuid();
    String templateUri = null;
    if (templateUuid != null) {
      // Step is linked
      templateUri = templateService.makeNamespacedTemplareUri(templateUuid, step.getTemplateVersion());
      if (templateUri != null) {
        Template template = templateService.get(templateUuid);
        List<String> templateProperties = templateService.fetchTemplateProperties(template);
        if (templateProperties != null && outputProperties != null) {
          outputProperties.keySet().removeAll(templateProperties);
        }
        if (TemplateType.SSH.toString().equals(template.getType())) {
          outputProperties.put("commandType", CommandType.OTHER.name());
        }
      }
    }

    if (StateType.HTTP.name().equals(step.getType()) && isNotEmpty(outputProperties)) {
      outputProperties.remove("header");
    }

    if (StateType.APPROVAL.name().equals(step.getType()) && isNotEmpty(outputProperties)) {
      if (ApprovalStateType.SERVICENOW.name().equals(properties.get(APPROVAL_STATE_TYPE_VARIABLE))) {
        Map<String, Object> snowParams =
            (Map<String, Object>) ((Map<String, Object>) properties.get(ApprovalStateKeys.approvalStateParams))
                .get("serviceNowApprovalParams");
        if (snowParams.containsKey("approval") || snowParams.containsKey("rejection")) {
          snowParams.keySet().removeAll(Arrays.asList("approvalValue", "rejectionValue", "approvalField",
              "rejectionField", "approvalOperator", "rejectionOperator"));
        }
      }
    }

    if (StateType.SERVICENOW_CREATE_UPDATE.name().equals(step.getType())) {
      Set<String> serviceNowCreateUpdateParams =
          ((Map<String, Object>) outputProperties.get(SERVICE_NOW_CREATE_UPDATE_PARAMS)).keySet();
      Set<String> snowCreateUpdateParamsFields = getSnowCreateUpdateParamsFields(outputProperties);
      Set<String> snowCreateUpdateParamsAdditionalFields = getSnowCreateUpdateParamsAdditionalFields(outputProperties);
      outputProperties.entrySet().removeIf(entry
          -> serviceNowCreateUpdateParams.contains(entry.getKey())
              || snowCreateUpdateParamsFields.contains(entry.getKey())
              || snowCreateUpdateParamsAdditionalFields.contains(entry.getKey()));
    }

    return StepYaml.builder()
        .name(step.getName())
        .properties(outputProperties == null || outputProperties.isEmpty() ? null : outputProperties)
        .type(step.getType())
        .templateExpressions(templateExprYamlList)
        .templateUri(templateUri)
        .templateVariables(TemplateHelper.convertToTemplateVariables(step.getTemplateVariables()))
        .build();
  }

  private Set<String> getSnowCreateUpdateParamsFields(Map<String, Object> outputProperties) {
    Map<String, Object> fields =
        (Map<String, Object>) ((Map<String, Object>) outputProperties.get(SERVICE_NOW_CREATE_UPDATE_PARAMS))
            .get("fields");
    return isEmpty(fields) ? Collections.emptySet() : fields.keySet();
  }

  private Set<String> getSnowCreateUpdateParamsAdditionalFields(Map<String, Object> outputProperties) {
    Map<String, Object> additionalFields =
        (Map<String, Object>) ((Map<String, Object>) outputProperties.get(SERVICE_NOW_CREATE_UPDATE_PARAMS))
            .get("additionalFields");
    return isEmpty(additionalFields) ? Collections.emptySet() : additionalFields.keySet();
  }

  // If the properties contain known entity id, convert it into name
  private void convertIdToNameIfKnownType(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    if (isEmpty(name)) {
      return;
    }

    switch (name) {
      case "computeProviderId":
        String computeProviderId = (String) objectValue;
        SettingAttribute settingAttribute = settingsService.get(computeProviderId);
        notNullCheck("Setting Attribute is null for the given id:" + computeProviderId, settingAttribute, USER);
        outputProperties.put("computeProviderName", settingAttribute.getName());
        return;
      case "serviceId":
        String serviceId = (String) objectValue;
        Service service = serviceResourceService.getWithDetails(appId, serviceId);
        notNullCheck("Service is null for the given id:" + serviceId, service, USER);
        outputProperties.put("serviceName", service.getName());
        return;
      case "infraMappingId":
        String infraMappingId = (String) objectValue;
        InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingId);
        notNullCheck("Infra mapping is null for the given id:" + infraMappingId, infraMapping, USER);
        outputProperties.put("infraMappingName", infraMapping.getName());
        return;
      case "artifactStreamId":
        String artifactStreamId = (String) objectValue;
        ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
        notNullCheck("Artifact stream is null for the given id:" + artifactStreamId, artifactStream, USER);
        outputProperties.put("artifactStreamName", artifactStream.getName());

        if (inputProperties.get("serviceId") == null) {
          Service serviceWithArtifactStream =
              artifactStreamServiceBindingService.getService(appId, artifactStreamId, false);
          notNullCheck(
              "Service is null for the given artifactStreamId:" + artifactStreamId, serviceWithArtifactStream, USER);
          outputProperties.put("serviceName", serviceWithArtifactStream.getName());
        }
        return;
      case "provisionerId":
        String provisionerId = (String) objectValue;
        InfrastructureProvisioner provisioner = infrastructureProvisionerService.get(appId, provisionerId);
        notNullCheck("Provisioner is null for the given provisionerId:" + provisionerId, provisioner, USER);
        outputProperties.put("provisionerName", provisioner.getName());
        return;
      default:
        outputProperties.put(name, objectValue);
        return;
    }
  }

  // If the properties contain known entity type, convert the name back to id, this is used in toBean() path
  private void convertNameToIdIfKnownType(String name, Object objectValue, Map<String, Object> properties, String appId,
      String accountId, Map<String, Object> inputProperties) {
    if (isEmpty(name)) {
      return;
    }
    Object serviceNameObj;
    String sourceType;
    switch (name) {
      case "computeProviderName":
        String computeProviderName = (String) objectValue;
        SettingAttribute settingAttribute = settingsService.getSettingAttributeByName(accountId, computeProviderName);
        notNullCheck("Setting Attribute is null for the given name:" + computeProviderName, settingAttribute, USER);
        properties.put("computeProviderId", settingAttribute.getUuid());
        return;
      case "serviceName":
        String serviceName = (String) objectValue;
        Service service = serviceResourceService.getServiceByName(appId, serviceName);
        notNullCheck("Service is null for the given name:" + serviceName, service, USER);
        properties.put("serviceId", service.getUuid());
        return;
      case "infraMappingName":
        String infraMappingName = (String) objectValue;
        InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingName);
        notNullCheck("Infra mapping is null for the given name:" + infraMappingName, infraMapping, USER);
        properties.put("infraMappingId", infraMapping.getUuid());
        return;
      case "artifactStreamName":
        sourceType = inputProperties.get("sourceType") == null ? null : inputProperties.get("sourceType").toString();
        if (CollectionEntityType.MANIFEST.name().equals(sourceType)) {
          return;
        }
        String artifactStreamName = (String) objectValue;
        serviceNameObj = inputProperties.get("serviceName");
        notNullCheck("Service null in the properties", serviceNameObj, USER);
        serviceName = (String) serviceNameObj;
        service = serviceResourceService.getServiceByName(appId, serviceName);
        notNullCheck("Service is null for the given name:" + serviceName, service, USER);
        ArtifactStream artifactStream =
            artifactStreamService.getArtifactStreamByName(appId, service.getUuid(), artifactStreamName);
        notNullCheck("Artifact stream is null for the given name:" + artifactStreamName, artifactStream, USER);
        properties.put("artifactStreamId", artifactStream.getUuid());
        return;
      case "appManifestName":
        sourceType = inputProperties.get("sourceType") == null ? null : inputProperties.get("sourceType").toString();
        if (!CollectionEntityType.MANIFEST.name().equals(sourceType)) {
          return;
        }
        String appManifestName = (String) objectValue;
        serviceNameObj = inputProperties.get("serviceName");
        notNullCheck("Service null in the properties", serviceNameObj, USER);
        serviceName = (String) serviceNameObj;
        service = serviceResourceService.getServiceByName(appId, serviceName);
        notNullCheck("Service is null for the given name:" + serviceName, service, USER);
        ApplicationManifest applicationManifest =
            applicationManifestService.getAppManifestByName(appId, null, service.getUuid(), appManifestName);
        notNullCheck("Application Manifest is null for the given name:" + appManifestName, applicationManifest, USER);
        properties.put("appManifestId", applicationManifest.getUuid());
        return;
      case "provisionerName":
        String provisionerName = (String) objectValue;
        InfrastructureProvisioner provisioner = infrastructureProvisionerService.getByName(appId, provisionerName);
        notNullCheck("Provisioner is null for the given name:" + provisionerName, provisioner, USER);
        properties.put("provisionerId", provisioner.getUuid());
        return;
      default:
        properties.put(name, objectValue);
        return;
    }
  }

  // Some of these properties need not be exposed, they could be generated in the toBean() method
  private boolean shouldBeIgnored(String name) {
    if (isEmpty(name)) {
      return true;
    }

    switch (name) {
      case "id":
      case "parentId":
      case "subWorkflowId":
        return true;
      default:
        return false;
    }
  }

  @Override
  public GraphNode upsertFromYaml(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
  }

  @Override
  public Class getYamlClass() {
    return StepYaml.class;
  }

  @Override
  public GraphNode get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<StepYaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
