package software.wings.service.intfc;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.SPOTINST;
import static software.wings.api.DeploymentType.WINRM;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.jexl3.JexlException;
import org.mongodb.morphia.query.Query;
import software.wings.api.DeploymentType;
import software.wings.beans.Event.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.infra.CloudProviderInfrastructure;
import software.wings.infra.FieldKeyValMapProvider;
import software.wings.infra.InfraDefinitionDetail;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class InfrastructureDefinitionServiceImpl implements InfrastructureDefinitionService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ManagerExpressionEvaluator evaluator;
  @Inject private SettingsService settingsService;
  @Inject private YamlPushService yamlPushService;

  @Override
  public PageResponse<InfrastructureDefinition> list(
      PageRequest<InfrastructureDefinition> pageRequest, String serviceId, String appId) {
    if (EmptyPredicate.isNotEmpty(serviceId)) {
      Service service = serviceResourceService.get(appId, serviceId);
      if (service == null) {
        throw new InvalidRequestException(format("No service exists for id : [%s]", serviceId));
      }
      if (service.getDeploymentType() != null) {
        pageRequest.addFilter(
            InfrastructureDefinitionKeys.deploymentType, Operator.EQ, service.getDeploymentType().name());
      }
      SearchFilter op1 = SearchFilter.builder()
                             .fieldName(InfrastructureDefinitionKeys.scopedToServices)
                             .op(Operator.NOT_EXISTS)
                             .build();
      SearchFilter op2 = SearchFilter.builder()
                             .fieldName(InfrastructureDefinitionKeys.scopedToServices)
                             .op(Operator.CONTAINS)
                             .fieldValues(new Object[] {serviceId})
                             .build();
      pageRequest.addFilter(InfrastructureDefinitionKeys.scopedToServices, Operator.OR, new Object[] {op1, op2});
    }
    return wingsPersistence.query(InfrastructureDefinition.class, pageRequest);
  }

  @Override
  public InfrastructureDefinition save(InfrastructureDefinition infrastructureDefinition) {
    String accountId = appService.getAccountIdByAppId(infrastructureDefinition.getAppId());
    validateInfraDefinition(infrastructureDefinition);
    String uuid;
    try {
      uuid = wingsPersistence.save(infrastructureDefinition);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(
          format("Infra definition already exists with the name : [%s]", infrastructureDefinition.getName()),
          WingsException.USER);
    }
    infrastructureDefinition.setUuid(uuid);
    // TODO: look at git sync once support is added
    yamlPushService.pushYamlChangeSet(accountId, null, infrastructureDefinition, Type.CREATE, false, false);
    return infrastructureDefinition;
  }

  private void validateInfraDefinition(@Valid InfrastructureDefinition infraDefinition) {
    InfrastructureMapping infrastructureMapping = infraDefinition.getInfraMapping();
    // Some Hack To validate without Service Template
    infrastructureMapping.setServiceTemplateId("dummy");
    infrastructureMapping.setAccountId(appService.getAccountIdByAppId(infraDefinition.getAppId()));
    infrastructureMappingService.validateInfraMapping(infrastructureMapping, false);
  }

  @Override
  public InfrastructureDefinition get(String appId, String infraDefinitionId) {
    return wingsPersistence.getWithAppId(InfrastructureDefinition.class, appId, infraDefinitionId);
  }

  @Override
  public InfrastructureDefinition update(InfrastructureDefinition infrastructureDefinition) {
    String accountId = appService.getAccountIdByAppId(infrastructureDefinition.getAppId());
    validateInfraDefinition(infrastructureDefinition);
    InfrastructureDefinition savedInfraDefinition =
        get(infrastructureDefinition.getAppId(), infrastructureDefinition.getUuid());
    if (savedInfraDefinition == null) {
      throw new InvalidRequestException(
          format("Infra Definition does not exist with id: [%s]", infrastructureDefinition.getUuid()));
    }

    try {
      wingsPersistence.save(infrastructureDefinition);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(
          format("Infra definition already exists with the name : [%s]", infrastructureDefinition.getName()),
          WingsException.USER);
    }
    boolean rename = !infrastructureDefinition.getName().equals(savedInfraDefinition.getName());
    yamlPushService.pushYamlChangeSet(
        accountId, savedInfraDefinition, infrastructureDefinition, Type.UPDATE, false, rename);
    return infrastructureDefinition;
  }

  @Override
  public void delete(String appId, String infraDefinitionId) {
    String accountId = appService.getAccountIdByAppId(appId);
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    wingsPersistence.delete(InfrastructureDefinition.class, appId, infraDefinitionId);
    yamlPushService.pushYamlChangeSet(accountId, infraDefinitionId, null, Type.DELETE, false, false);
  }

  @Override
  public void deleteByYamlGit(String appid, String infraDefinitionId) {
    delete(appid, infraDefinitionId);
  }

  @Override
  public Map<DeploymentType, List<SettingVariableTypes>> getDeploymentTypeCloudProviderOptions() {
    Map<DeploymentType, List<SettingVariableTypes>> deploymentCloudProviderOptions = new HashMap<>();

    deploymentCloudProviderOptions.put(DeploymentType.SSH,
        asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
    deploymentCloudProviderOptions.put(KUBERNETES,
        asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
    deploymentCloudProviderOptions.put(
        HELM, asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
    deploymentCloudProviderOptions.put(ECS, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(AWS_CODEDEPLOY, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(AWS_LAMBDA, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(AMI, asList(SettingVariableTypes.AWS));
    deploymentCloudProviderOptions.put(
        WINRM, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
    deploymentCloudProviderOptions.put(PCF, asList(SettingVariableTypes.PCF));
    deploymentCloudProviderOptions.put(SPOTINST, asList(SettingVariableTypes.SPOT_INST));

    return deploymentCloudProviderOptions;
  }

  @Override
  public InfrastructureMapping getInfraMapping(
      String appId, String serviceId, String infraDefinitionId, ExecutionContext context) {
    validateInputs(appId, serviceId, infraDefinitionId);
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      throw new InvalidRequestException(format(
          "No infra definition exists with given appId: [%s] infra definition id : [%s]", appId, infraDefinitionId));
    }
    renderExpression(infrastructureDefinition, context);
    InfrastructureMapping infraMapping = existingInfraMapping(infrastructureDefinition, serviceId);
    if (infraMapping != null) {
      return infraMapping;
    } else {
      infraMapping = infrastructureDefinition.getInfraMapping();
      infraMapping.setServiceId(serviceId);
      infraMapping.setAccountId(appService.getAccountIdByAppId(appId));
      ServiceTemplate serviceTemplate = serviceTemplateService.get(
          infrastructureDefinition.getAppId(), serviceId, infrastructureDefinition.getEnvId());
      infraMapping.setServiceTemplateId(serviceTemplate.getUuid());
      infraMapping.setInfrastructureDefinitionId(infraDefinitionId);
      infraMapping.setAutoPopulate(true);
      return infrastructureMappingService.save(infraMapping);
    }
  }

  private void renderExpression(InfrastructureDefinition infrastructureDefinition, ExecutionContext context) {
    Map<String, Object> fieldMapForClass =
        ((FieldKeyValMapProvider) infrastructureDefinition.getInfrastructure()).getFieldMapForClass();
    for (Entry<String, Object> entry : fieldMapForClass.entrySet()) {
      if (entry.getValue() instanceof String) {
        entry.setValue(context.renderExpression((String) entry.getValue()));
      } else if (entry.getValue() instanceof List) {
        List result = new ArrayList();
        for (Object o : (List) entry.getValue()) {
          if (o instanceof String) {
            result.addAll(getList(context.renderExpression((String) o)));
          } else {
            result.add(o);
          }
        }
        entry.setValue(result);
      }
    }
    saveFieldMapForDefinition(infrastructureDefinition, fieldMapForClass);
  }

  private List getList(Object input) {
    if (input instanceof String) {
      return Arrays.asList(((String) input).split(","));
    }
    return (List) input;
  }

  private void saveFieldMapForDefinition(
      InfrastructureDefinition infrastructureDefinition, Map<String, Object> fieldMapForClass) {
    try {
      Class<? extends InfraMappingInfrastructureProvider> aClass =
          infrastructureDefinition.getInfrastructure().getClass();
      for (Entry<String, Object> entry : fieldMapForClass.entrySet()) {
        Field field = aClass.getDeclaredField(entry.getKey());
        field.setAccessible(true);
        field.set(infrastructureDefinition.getInfrastructure(), entry.getValue());
      }
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.GENERAL_ERROR).addParam("message", ExceptionUtils.getMessage(ex));
    }
  }

  @Override
  public boolean isDynamicInfrastructure(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      return false;
    }
    return isNotEmpty(infrastructureDefinition.getProvisionerId());
  }

  @Override
  public List<String> fetchCloudProviderIds(String appId, List<String> infraDefinitionIds) {
    if (isNotEmpty(infraDefinitionIds)) {
      List<InfrastructureDefinition> infrastructureDefinitions =
          wingsPersistence.createQuery(InfrastructureDefinition.class)
              .project(InfrastructureDefinitionKeys.appId, true)
              .project(InfrastructureDefinitionKeys.infrastructure, true)
              .filter(InfrastructureDefinitionKeys.appId, appId)
              .field(InfrastructureDefinitionKeys.uuid)
              .in(infraDefinitionIds)
              .asList();
      return infrastructureDefinitions.stream()
          .map(InfrastructureDefinition::getInfrastructure)
          .map(CloudProviderInfrastructure::getCloudProviderId)
          .distinct()
          .collect(toList());
    }
    return new ArrayList<>();
  }

  @Override
  public InfrastructureDefinition getInfraDefByName(String appId, String envId, String infraDefName) {
    return wingsPersistence.createQuery(InfrastructureDefinition.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter("name", infraDefName)
        .get();
  }

  @VisibleForTesting
  private InfrastructureMapping existingInfraMapping(InfrastructureDefinition infraDefinition, String serviceId) {
    InfraMappingInfrastructureProvider infrastructure = infraDefinition.getInfrastructure();
    Class<? extends InfrastructureMapping> mappingClass = infrastructure.getMappingClass();
    Map<String, Object> queryMap = ((FieldKeyValMapProvider) infrastructure).getFieldMapForClass();
    Query baseQuery =
        wingsPersistence.createQuery(mappingClass)
            .filter(InfrastructureMapping.APP_ID_KEY, infraDefinition.getAppId())
            .filter(InfrastructureMapping.ENV_ID_KEY, infraDefinition.getEnvId())
            .filter(InfrastructureMappingKeys.serviceId, serviceId)
            .filter(InfrastructureMappingKeys.computeProviderSettingId, infrastructure.getCloudProviderId())
            .filter(InfrastructureMappingKeys.infrastructureDefinitionId, infraDefinition.getUuid());
    queryMap.forEach(baseQuery::filter);
    List<InfrastructureMapping> infrastructureMappings = baseQuery.asList();
    if (isEmpty(infrastructureMappings)) {
      return null;
    } else {
      if (infrastructureMappings.size() > 1) {
        throw new WingsException(format("More than 1 mappings found for infra definition : [%s]. Mappings : [%s",
            infraDefinition.toString(), infrastructureMappings.toString()));
      }
      return infrastructureMappings.get(0);
    }
  }

  private void validateInputs(String appId, String serviceId, String infraDefinitionId) {
    if (isEmpty(appId)) {
      throw new InvalidRequestException("App Id can't be empty");
    }
    if (isEmpty(serviceId)) {
      throw new InvalidRequestException("Service Id can't be empty");
    }
    if (isEmpty(infraDefinitionId)) {
      throw new InvalidRequestException("Infra Definition Id can't be empty");
    }
  }

  @Override
  public List<InfrastructureDefinition> getInfraStructureDefinitionByUuids(
      String appId, List<String> infraDefinitionIds) {
    if (isNotEmpty(infraDefinitionIds)) {
      return wingsPersistence.createQuery(InfrastructureDefinition.class)
          .filter(InfrastructureDefinitionKeys.appId, appId)
          .field("uuid")
          .in(infraDefinitionIds)
          .asList();
    }
    return new ArrayList<>();
  }

  @Override
  public String cloudProviderNameForDefinition(InfrastructureDefinition infrastructureDefinition) {
    SettingAttribute settingAttribute =
        settingsService.get(infrastructureDefinition.getInfrastructure().getCloudProviderId());
    if (settingAttribute != null) {
      return settingAttribute.getName();
    }
    return null;
  }

  @Override
  public String cloudProviderNameForDefinition(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    return cloudProviderNameForDefinition(infrastructureDefinition);
  }

  @Override
  public InfraDefinitionDetail getDetail(String appId, String infraDefinitionId) {
    InfraDefinitionDetail infraDefinitionDetail = InfraDefinitionDetail.builder().build();
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    if (infrastructureDefinition == null) {
      return infraDefinitionDetail;
    }
    infraDefinitionDetail.setInfrastructureDefinition(infrastructureDefinition);
    infraDefinitionDetail.setDerivedInfraMappings(getMappings(infrastructureDefinition.getUuid(), appId));

    return infraDefinitionDetail;
  }

  private List<InfrastructureMapping> getMappings(String infraDefinitionId, String appId) {
    return wingsPersistence.createQuery(InfrastructureMapping.class)
        .filter(InfrastructureMappingKeys.appId, appId)
        .filter(InfrastructureMappingKeys.infrastructureDefinitionId, infraDefinitionId)
        .asList();
  }

  private Object evaluateExpression(String expression, Map<String, Object> contextMap) {
    try {
      return evaluator.evaluate(expression, contextMap);
    } catch (JexlException.Variable ex) {
      // Do nothing.
    }
    return null;
  }

  @Override
  public void applyProvisionerOutputs(
      InfrastructureDefinition infrastructureDefinition, Map<String, Object> contextMap) {
    Map<String, Object> fieldMapForClass =
        ((FieldKeyValMapProvider) infrastructureDefinition.getInfrastructure()).getFieldMapForClass();
    for (Entry<String, Object> entry : fieldMapForClass.entrySet()) {
      if (entry.getValue() instanceof String) {
        Object evaluated = evaluateExpression((String) entry.getValue(), contextMap);
        if (evaluated instanceof String) {
          entry.setValue(evaluated);
        }
      }
    }
    saveFieldMapForDefinition(infrastructureDefinition, fieldMapForClass);
  }
}
