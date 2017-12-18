package software.wings.service.impl.yaml.handler.workflow;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.beans.ErrorCode;
import software.wings.beans.Graph.Node;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.Yaml;
import software.wings.beans.ObjectType;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.common.UUIDGenerator;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Util;
import software.wings.utils.Validator;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
public class StepYamlHandler extends BaseYamlHandler<StepYaml, Node> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject SettingsService settingsService;
  @Inject InfrastructureMappingService infraMappingService;

  private Node toBean(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    StepYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    // template expressions
    List<TemplateExpression> templateExpressions = Lists.newArrayList();
    if (yaml.getTemplateExpressions() != null) {
      BaseYamlHandler templateExprYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
      templateExpressions = yaml.getTemplateExpressions()
                                .stream()
                                .map(templateExpr -> {
                                  try {
                                    ChangeContext.Builder clonedContext =
                                        cloneFileChangeContext(changeContext, templateExpr);
                                    return (TemplateExpression) templateExprYamlHandler.upsertFromYaml(
                                        clonedContext.build(), changeContextList);
                                  } catch (HarnessException e) {
                                    throw new WingsException(e);
                                  }
                                })
                                .collect(Collectors.toList());
    }

    // properties
    Map<String, Object> properties;
    List<NameValuePair> nameValuePairList = Lists.newArrayList();
    if (yaml.getProperties() != null) {
      nameValuePairList = yaml.getProperties()
                              .stream()
                              .map(nvpYaml -> {
                                NameValuePair nameValuePair =
                                    NameValuePair.builder().name(nvpYaml.getName()).value(nvpYaml.getValue()).build();
                                convertNameToIdIfKnownType(nameValuePair, appId, accountId);
                                return nameValuePair;
                              })
                              .collect(Collectors.toList());
    }

    generateKnownProperties(nameValuePairList, changeContext);

    properties = Util.toProperties(nameValuePairList);

    return Node.Builder.aNode()
        .withName(yaml.getName())
        .withType(yaml.getType())
        .withRollback(yaml.isRollback())
        .withTemplateExpressions(templateExpressions)
        .withProperties(properties)
        .build();
  }

  private void generateKnownProperties(List<NameValuePair> nameValuePairList, ChangeContext<StepYaml> changeContext) {
    String id = UUIDGenerator.getUuid();

    String phaseStepId = changeContext.getEntityIdMap().get("PHASE_STEP");
    NameValuePair idProperty = NameValuePair.builder().name("id").value(id).valueType("String").build();
    NameValuePair parentId = NameValuePair.builder().name("parentId").value(phaseStepId).valueType("String").build();
    NameValuePair subWorkflowId = NameValuePair.builder().name("subWorkflowId").value(id).valueType("String").build();
    nameValuePairList.add(idProperty);
    nameValuePairList.add(parentId);
    nameValuePairList.add(subWorkflowId);
  }

  @Override
  public StepYaml toYaml(Node bean, String appId) {
    List<NameValuePair.Yaml> nameValuePairYamlList = Lists.newArrayList();
    if (bean.getProperties() != null) {
      List<NameValuePair> nameValuePairs = Util.toYamlList(bean.getProperties());
      // properties
      BaseYamlHandler nameValuePairYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR, ObjectType.NAME_VALUE_PAIR);
      nameValuePairs.stream().forEach(nameValuePair -> {
        if (!shouldBeIgnored(nameValuePair)) {
          nameValuePair = convertIdToNameIfKnownType(nameValuePair, appId);
          Yaml yaml = (Yaml) nameValuePairYamlHandler.toYaml(nameValuePair, appId);
          nameValuePairYamlList.add(yaml);
        }
      });
    }

    // template expressions
    List<TemplateExpression.Yaml> templateExprYamlList = Lists.newArrayList();
    if (bean.getTemplateExpressions() != null) {
      BaseYamlHandler templateExpressionYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.TEMPLATE_EXPRESSION, ObjectType.TEMPLATE_EXPRESSION);
      templateExprYamlList =
          bean.getTemplateExpressions()
              .stream()
              .map(templateExpression
                  -> (TemplateExpression.Yaml) templateExpressionYamlHandler.toYaml(templateExpression, appId))
              .collect(Collectors.toList());
    }

    return StepYaml.builder()
        .name(bean.getName())
        .properties(nameValuePairYamlList)
        .rollback(bean.getRollback())
        .type(bean.getType())
        .templateExpressions(templateExprYamlList)
        .build();
  }

  // If the properties contain known entity id, convert it into name
  private NameValuePair convertIdToNameIfKnownType(NameValuePair nameValuePair, String appId) {
    String name = nameValuePair.getName();
    if (Util.isEmpty(name)) {
      return nameValuePair;
    }

    String entityId = nameValuePair.getValue();
    switch (name) {
      case "computeProviderId":
        SettingAttribute settingAttribute = settingsService.get(entityId);
        Validator.notNullCheck("Setting Attribute is null for the given id:" + entityId, settingAttribute);
        nameValuePair.setValue(settingAttribute.getName());
        nameValuePair.setName("computeProviderName");
        return nameValuePair;
      case "serviceId":
        Service service = serviceResourceService.get(appId, entityId);
        Validator.notNullCheck("Service is null for the given id:" + entityId, service);
        nameValuePair.setValue(service.getName());
        nameValuePair.setName("serviceName");
        return nameValuePair;
      case "infraMappingId":
        InfrastructureMapping infraMapping = infraMappingService.get(appId, entityId);
        Validator.notNullCheck("Infra mapping is null for the given id:" + entityId, infraMapping);
        nameValuePair.setValue(infraMapping.getName());
        nameValuePair.setName("infraMappingName");
        return nameValuePair;
      default:
        return nameValuePair;
    }
  }

  // If the properties contain known entity type, convert the name back to id, this is used in toBean() path
  private void convertNameToIdIfKnownType(NameValuePair nameValuePair, String appId, String accountId) {
    String name = nameValuePair.getName();
    if (Util.isEmpty(name)) {
      return;
    }

    String entityName = nameValuePair.getValue();
    switch (name) {
      case "computeProviderName":
        SettingAttribute settingAttribute = settingsService.getSettingAttributeByName(accountId, entityName);
        Validator.notNullCheck("Setting Attribute is null for the given name:" + entityName, settingAttribute);
        nameValuePair.setValue(settingAttribute.getUuid());
        nameValuePair.setName("computeProviderId");
        return;
      case "serviceName":
        Service service = serviceResourceService.getServiceByName(appId, entityName);
        Validator.notNullCheck("Service is null for the given name:" + entityName, service);
        nameValuePair.setValue(service.getUuid());
        nameValuePair.setName("serviceId");
        return;
      case "infraMappingName":
        InfrastructureMapping infraMapping = infraMappingService.get(appId, entityName);
        Validator.notNullCheck("Infra mapping is null for the given name:" + entityName, infraMapping);
        nameValuePair.setValue(infraMapping.getUuid());
        nameValuePair.setName("infraMappingId");
        return;
      default:
        return;
    }
  }

  // Some of these properties need not be exposed, they could be generated in the toBean() method
  private boolean shouldBeIgnored(NameValuePair nameValuePair) {
    String name = nameValuePair.getName();
    if (Util.isEmpty(name)) {
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
  public Node upsertFromYaml(ChangeContext<StepYaml> changeContext, List<ChangeContext> changeSetContext)
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
  public Node get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<StepYaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
