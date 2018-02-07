package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.common.UUIDGenerator;
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
import software.wings.utils.Validator;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class StepYamlHandler extends BaseYamlHandler<StepYaml, GraphNode> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ArtifactStreamService artifactStreamService;

  private GraphNode toBean(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    StepYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    // template expressions
    List<TemplateExpression> templateExpressions = Lists.newArrayList();
    if (yaml.getTemplateExpressions() != null) {
      TemplateExpressionYamlHandler templateExprYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
      templateExpressions =
          yaml.getTemplateExpressions()
              .stream()
              .map(templateExpr -> {
                try {
                  ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, templateExpr);
                  return templateExprYamlHandler.upsertFromYaml(clonedContext.build(), changeContextList);
                } catch (HarnessException e) {
                  throw new WingsException(e);
                }
              })
              .collect(Collectors.toList());
    }

    // properties
    Map<String, Object> outputProperties = Maps.newHashMap();

    Map<String, Object> yamlProperties = yaml.getProperties();
    if (yamlProperties != null) {
      yamlProperties.entrySet().stream().forEach(
          entry -> convertNameToIdIfKnownType(entry, outputProperties, appId, accountId, yamlProperties));
    }

    generateKnownProperties(outputProperties, changeContext);
    Boolean isRollback = (Boolean) changeContext.getProperties().get(YamlConstants.IS_ROLLBACK);
    return aGraphNode()
        .withName(yaml.getName())
        .withType(yaml.getType())
        .withTemplateExpressions(templateExpressions)
        .withRollback(isRollback)
        .withProperties(outputProperties.isEmpty() ? null : outputProperties)
        .build();
  }

  private void generateKnownProperties(Map<String, Object> outputProperties, ChangeContext<StepYaml> changeContext) {
    String id = UUIDGenerator.getUuid();

    String phaseStepId = changeContext.getEntityIdMap().get("PHASE_STEP");
    outputProperties.put("id", id);
    outputProperties.put("parentId", phaseStepId);
    outputProperties.put("subWorkflowId", id);
  }

  @Override
  public StepYaml toYaml(GraphNode bean, String appId) {
    Map<String, Object> properties = bean.getProperties();
    final Map<String, Object> outputProperties = Maps.newHashMap();
    if (properties != null) {
      properties.entrySet().stream().forEach(entry -> {
        if (!shouldBeIgnored(entry.getKey())) {
          convertIdToNameIfKnownType(entry, outputProperties, appId, properties);
        }
      });
    }

    // template expressions
    List<TemplateExpression.Yaml> templateExprYamlList = Lists.newArrayList();
    if (bean.getTemplateExpressions() != null) {
      TemplateExpressionYamlHandler templateExpressionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION);
      templateExprYamlList =
          bean.getTemplateExpressions()
              .stream()
              .map(templateExpression -> templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(Collectors.toList());
    }

    return StepYaml.builder()
        .name(bean.getName())
        .properties(outputProperties.isEmpty() ? null : outputProperties)
        .type(bean.getType())
        .templateExpressions(templateExprYamlList)
        .build();
  }

  // If the properties contain known entity id, convert it into name
  private void convertIdToNameIfKnownType(Entry<String, Object> mapEntry, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    String name = mapEntry.getKey();
    if (isEmpty(name)) {
      return;
    }

    Object objectValue = mapEntry.getValue();

    switch (name) {
      case "computeProviderId":
        String computeProviderId = (String) objectValue;
        SettingAttribute settingAttribute = settingsService.get(computeProviderId);
        Validator.notNullCheck("Setting Attribute is null for the given id:" + computeProviderId, settingAttribute);
        outputProperties.put("computeProviderName", settingAttribute.getName());
        return;
      case "serviceId":
        String serviceId = (String) objectValue;
        Service service = serviceResourceService.get(appId, serviceId);
        Validator.notNullCheck("Service is null for the given id:" + serviceId, service);
        outputProperties.put("serviceName", service.getName());
        return;
      case "infraMappingId":
        String infraMappingId = (String) objectValue;
        InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingId);
        Validator.notNullCheck("Infra mapping is null for the given id:" + infraMappingId, infraMapping);
        outputProperties.put("infraMappingName", infraMapping.getName());
        return;
      case "artifactStreamId":
        String artifactStreamId = (String) objectValue;
        ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
        Validator.notNullCheck("Artifact stream is null for the given id:" + artifactStreamId, artifactStream);
        outputProperties.put("artifactStreamName", artifactStream.getName());

        if (inputProperties.get("serviceId") == null) {
          String serviceIdWithArtifactStream = artifactStream.getServiceId();
          Service serviceWithArtifactStream = serviceResourceService.get(appId, serviceIdWithArtifactStream);
          Validator.notNullCheck(
              "Service is null for the given id:" + serviceIdWithArtifactStream, serviceWithArtifactStream);
          outputProperties.put("serviceName", serviceWithArtifactStream.getName());
        }
        return;
      default:
        outputProperties.put(name, objectValue);
        return;
    }
  }

  // If the properties contain known entity type, convert the name back to id, this is used in toBean() path
  private void convertNameToIdIfKnownType(Entry<String, Object> mapEntry, Map<String, Object> properties, String appId,
      String accountId, Map<String, Object> inputProperties) {
    String name = mapEntry.getKey();
    if (isEmpty(name)) {
      return;
    }

    Object objectValue = mapEntry.getValue();

    switch (name) {
      case "computeProviderName":
        String computeProviderName = (String) objectValue;
        SettingAttribute settingAttribute = settingsService.getSettingAttributeByName(accountId, computeProviderName);
        Validator.notNullCheck("Setting Attribute is null for the given name:" + computeProviderName, settingAttribute);
        properties.put("computeProviderId", settingAttribute.getUuid());
        return;
      case "serviceName":
        String serviceName = (String) objectValue;
        Service service = serviceResourceService.getServiceByName(appId, serviceName);
        Validator.notNullCheck("Service is null for the given name:" + serviceName, service);
        properties.put("serviceId", service.getUuid());
        return;
      case "infraMappingName":
        String infraMappingName = (String) objectValue;
        InfrastructureMapping infraMapping = infraMappingService.get(appId, infraMappingName);
        Validator.notNullCheck("Infra mapping is null for the given name:" + infraMappingName, infraMapping);
        properties.put("infraMappingId", infraMapping.getUuid());
        return;
      case "artifactStreamName":
        String artifactStreamName = (String) objectValue;
        Object serviceNameObj = inputProperties.get("serviceName");
        Validator.notNullCheck("Service null in the properties", serviceNameObj);
        serviceName = (String) serviceNameObj;
        service = serviceResourceService.getServiceByName(appId, serviceName);
        Validator.notNullCheck("Service is null for the given name:" + serviceName, service);
        ArtifactStream artifactStream =
            artifactStreamService.getArtifactStreamByName(appId, service.getUuid(), artifactStreamName);
        Validator.notNullCheck("Artifact stream is null for the given name:" + artifactStreamName, artifactStream);
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
  public boolean validate(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext) {
    return false;
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
