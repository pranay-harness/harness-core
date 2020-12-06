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

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
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
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
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
import software.wings.yaml.workflow.StepYaml;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.commons.lang3.StringUtils;

/**
 * @author rktummala on 10/28/17
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
public class StepYamlHandler extends BaseYamlHandler<StepYaml, GraphNode> {
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
  @Inject private StepCompletionYamlValidatorFactory stepCompletionYamlValidatorFactory;

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

    if (isEmpty(stepYaml.getName())) {
      throw new InvalidRequestException("Step name is empty for " + stepYaml.getType() + " step");
    }

    StepCompletionYamlValidator stepCompletionYamlValidator =
        stepCompletionYamlValidatorFactory.getValidatorForStepType(StepType.valueOf(stepYaml.getType()));
    if (stepCompletionYamlValidator != null) {
      stepCompletionYamlValidator.validate(changeContext);
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
      yamlProperties.forEach(
          (name, value) -> convertNameToIdIfKnownType(name, value, outputProperties, appId, accountId, yamlProperties));
    }

    generateKnownProperties(outputProperties, changeContext);

    validateArtifactCollectionStep(stepYaml, outputProperties);

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
        .type(stepYaml.getType())
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

  private void validateArtifactCollectionStep(StepYaml stepYaml, Map<String, Object> outputProperties) {
    if (StateType.ARTIFACT_COLLECTION.name().equals(stepYaml.getType()) && isNotEmpty(outputProperties)) {
      String artifactStreamId = (String) outputProperties.get("artifactStreamId");
      if (artifactStreamId != null) {
        ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
        notNullCheck("Artifact stream is null for the given id:" + artifactStreamId, artifactStream, USER);
        validateMandatoryFieldsWithParameterizedArtifactStream(outputProperties, artifactStreamId, artifactStream);
      }
    }
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
    if (properties != null) {
      properties.forEach((name, value) -> {
        if (!shouldBeIgnored(name)) {
          convertIdToNameIfKnownType(name, value, outputProperties, appId, properties);
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

    return StepYaml.builder()
        .name(step.getName())
        .properties(outputProperties == null || outputProperties.isEmpty() ? null : outputProperties)
        .type(step.getType())
        .templateExpressions(templateExprYamlList)
        .templateUri(templateUri)
        .templateVariables(TemplateHelper.convertToTemplateVariables(step.getTemplateVariables()))
        .build();
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
      case GCB_OPTIONS:
        var gcbOptions = (Map<String, Object>) objectValue;
        String gcpConfigId = (String) gcbOptions.get(GCP_CONFIG_ID);
        if (gcpConfigId != null) {
          SettingAttribute gcpSettingAttribute = settingsService.get(gcpConfigId);
          notNullCheck("GCP is null for the given gcpConfigId:" + gcpConfigId, gcpSettingAttribute, USER);
          gcbOptions.put(GCP_CONFIG_NAME, gcpSettingAttribute.getName());
        } else {
          gcbOptions.put(GCP_CONFIG_NAME, null);
        }
        if (gcbOptions.containsKey(REPOSITORY_SPEC)) {
          var repositorySpec = (Map<String, Object>) gcbOptions.get(REPOSITORY_SPEC);
          String gitConfigId = (String) repositorySpec.get(GIT_CONFIG_ID);
          if (gitConfigId != null) {
            SettingAttribute gitSettingAttribute = settingsService.get(gitConfigId);
            notNullCheck("Git connector is null for the given gitConfigId:" + gitConfigId, gitSettingAttribute, USER);
            repositorySpec.put(GIT_CONFIG_NAME, gitSettingAttribute.getName());
          } else {
            repositorySpec.put(GIT_CONFIG_NAME, null);
          }
          repositorySpec.remove(GIT_CONFIG_ID);
        }
        gcbOptions.remove(GCP_CONFIG_ID);
        outputProperties.put(GCB_OPTIONS, gcbOptions);
        return;
      case JENKINS_ID:
        if (objectValue != null) {
          String jenkinsConfigId = (String) objectValue;
          SettingAttribute jenkinsConfig = settingsService.get(jenkinsConfigId);
          notNullCheck("Jenkins is null for the given jenkinsConfigId:" + jenkinsConfigId, jenkinsConfig, USER);
          outputProperties.put(JENKINS_NAME, jenkinsConfig.getName());
        } else {
          outputProperties.put(JENKINS_NAME, null);
        }
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
        String artifactStreamName = (String) objectValue;
        Object serviceNameObj = inputProperties.get("serviceName");
        notNullCheck("Service null in the properties", serviceNameObj, USER);
        serviceName = (String) serviceNameObj;
        service = serviceResourceService.getServiceByName(appId, serviceName);
        notNullCheck("Service is null for the given name:" + serviceName, service, USER);
        ArtifactStream artifactStream =
            artifactStreamService.getArtifactStreamByName(appId, service.getUuid(), artifactStreamName);
        notNullCheck("Artifact stream is null for the given name:" + artifactStreamName, artifactStream, USER);
        properties.put("artifactStreamId", artifactStream.getUuid());
        return;
      case "provisionerName":
        String provisionerName = (String) objectValue;
        InfrastructureProvisioner provisioner = infrastructureProvisionerService.getByName(appId, provisionerName);
        notNullCheck("Provisioner is null for the given name:" + provisionerName, provisioner, USER);
        properties.put("provisionerId", provisioner.getUuid());
        return;
      case GCB_OPTIONS:
        var gcbOptions = (Map<String, Object>) objectValue;
        if (gcbOptions.containsKey(GCP_CONFIG_NAME)) {
          String gcpConfigName = (String) gcbOptions.get(GCP_CONFIG_NAME);
          if (gcpConfigName != null) {
            SettingAttribute gcpSettingAttribute = settingsService.getSettingAttributeByName(accountId, gcpConfigName);
            notNullCheck("GCP is null for the given gcpConfigName:" + gcpConfigName, gcpSettingAttribute, USER);
            gcbOptions.put(GCP_CONFIG_ID, gcpSettingAttribute.getUuid());
          } else {
            gcbOptions.put(GCP_CONFIG_ID, null);
          }
          gcbOptions.remove(GCP_CONFIG_NAME);
        }
        if (gcbOptions.containsKey(REPOSITORY_SPEC)) {
          var repositorySpec = (Map<String, Object>) gcbOptions.get(REPOSITORY_SPEC);
          String gitConfigName = (String) repositorySpec.get(GIT_CONFIG_NAME);
          if (gitConfigName != null) {
            SettingAttribute gitSettingAttribute = settingsService.getSettingAttributeByName(accountId, gitConfigName);
            notNullCheck(
                "Git connector is null for the given gitConfigName:" + gitConfigName, gitSettingAttribute, USER);
            repositorySpec.put(GIT_CONFIG_ID, gitSettingAttribute.getUuid());
          } else {
            repositorySpec.put(GIT_CONFIG_ID, null);
          }
          repositorySpec.remove(GIT_CONFIG_NAME);
        }
        properties.put(GCB_OPTIONS, gcbOptions);
        return;
      case JENKINS_NAME:
        if (objectValue != null) {
          String jenkinsConfigName = (String) objectValue;
          SettingAttribute jenkinsConfig = settingsService.getSettingAttributeByName(accountId, jenkinsConfigName);
          notNullCheck("Jenkins is null for the given jenkinsConfigName:" + jenkinsConfigName, jenkinsConfig, USER);
          properties.put(JENKINS_ID, jenkinsConfig.getUuid());
        } else {
          properties.put(JENKINS_ID, null);
        }
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
