package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.template.TemplateHelper.convertToEntityVariables;
import static software.wings.beans.template.TemplateHelper.obtainTemplateVersion;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.template.TemplateExpressionYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.Map;
/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class StepYamlHandler extends BaseYamlHandler<StepYaml, GraphNode> {
  private static final Logger logger = LoggerFactory.getLogger(StepYamlHandler.class);
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject private TemplateService templateService;

  private GraphNode toBean(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    StepYaml stepYaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

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
    Map<String, Object> outputProperties = Maps.newHashMap();

    Map<String, Object> yamlProperties = stepYaml.getProperties();
    if (yamlProperties != null) {
      yamlProperties.forEach(
          (name, value) -> convertNameToIdIfKnownType(name, value, outputProperties, appId, accountId, yamlProperties));
    }

    generateKnownProperties(outputProperties, changeContext);
    Boolean isRollback = false;
    if (changeContext.getProperties().get(YamlConstants.IS_ROLLBACK) != null) {
      isRollback = (Boolean) changeContext.getProperties().get(YamlConstants.IS_ROLLBACK);
    }

    String templateUuid = null;
    String templateVersion = null;
    String templateUri = stepYaml.getTemplateUri();
    if (isNotEmpty(templateUri)) {
      templateUuid = templateService.fetchTemplateIdFromUri(accountId, templateUri);
      templateVersion = obtainTemplateVersion(templateUri);
    }
    return aGraphNode()
        .withName(stepYaml.getName())
        .withType(stepYaml.getType())
        .withTemplateExpressions(templateExpressions)
        .withRollback(isRollback)
        .withProperties(outputProperties.isEmpty() ? null : outputProperties)
        .withTemplateUuid(templateUuid)
        .withTemplateVersion(templateVersion)
        .withTemplateVariables(convertToEntityVariables(stepYaml.getTemplateVariables()))
        .build();
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
    final Map<String, Object> outputProperties = Maps.newHashMap();
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

    String templateUri = null;
    String templateUuid = step.getTemplateUuid();
    if (templateUuid != null) {
      // Step is linkedH
      templateUri = templateService.fetchTemplateUri(templateUuid);
      if (templateUri == null) {
        logger.error("Linked template for http template  {} was deleted ", templateUuid);
      }
      if (step.getTemplateVersion() != null) {
        templateUri = templateUri + ":" + step.getTemplateVersion();
      }
    }

    return StepYaml.builder()
        .name(step.getName())
        .properties(outputProperties.isEmpty() ? null : outputProperties)
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
        Service service = serviceResourceService.get(appId, serviceId);
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
        ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
        notNullCheck("Artifact stream is null for the given id:" + artifactStreamId, artifactStream, USER);
        outputProperties.put("artifactStreamName", artifactStream.getName());

        if (inputProperties.get("serviceId") == null) {
          String serviceIdWithArtifactStream = artifactStream.getServiceId();
          Service serviceWithArtifactStream = serviceResourceService.get(appId, serviceIdWithArtifactStream);
          notNullCheck(
              "Service is null for the given id:" + serviceIdWithArtifactStream, serviceWithArtifactStream, USER);
          outputProperties.put("serviceName", serviceWithArtifactStream.getName());
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
