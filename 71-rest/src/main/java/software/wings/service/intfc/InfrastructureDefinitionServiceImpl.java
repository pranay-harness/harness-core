package software.wings.service.intfc;

import static io.harness.beans.PageResponse.PageRequestBuilder;
import static io.harness.beans.PageResponse.PageResponseBuilder;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.SPOTINST;
import static software.wings.api.DeploymentType.WINRM;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ecs.model.LaunchType;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.mongodb.DuplicateKeyException;
import io.fabric8.utils.CountingMap;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.queue.Queue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentType;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsAmiInfrastructure.AwsAmiInfrastructureKeys;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsEcsInfrastructure.AwsEcsInfrastructureKeys;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.AwsInstanceInfrastructureKeys;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure.AwsLambdaInfrastructureKeys;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.CloudProviderInfrastructure;
import software.wings.infra.FieldKeyValMapProvider;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.GoogleKubernetesEngine.GoogleKubernetesEngineKeys;
import software.wings.infra.InfraDefinitionDetail;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.infra.ProvisionerAware;
import software.wings.infra.SshBasedInfrastructure;
import software.wings.infra.WinRmBasedInfrastructure;
import software.wings.prune.PruneEvent;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.intfc.aws.manager.AwsRoute53HelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.utils.Validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class InfrastructureDefinitionServiceImpl implements InfrastructureDefinitionService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ManagerExpressionEvaluator evaluator;
  @Inject private SettingsService settingsService;
  @Inject private YamlPushService yamlPushService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private SecretManager secretManager;
  @Inject private Map<String, InfrastructureProvider> infrastructureProviderMap;
  @Inject private AwsRoute53HelperServiceManager awsRoute53HelperServiceManager;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private TriggerService triggerService;

  @Inject private Queue<PruneEvent> pruneQueue;

  @Inject private AuditServiceHelper auditServiceHelper;

  @Override
  public PageResponse<InfrastructureDefinition> list(PageRequest<InfrastructureDefinition> pageRequest) {
    return wingsPersistence.query(InfrastructureDefinition.class, pageRequest);
  }

  @Override
  public PageResponse<InfrastructureDefinition> list(String appId, String envId, String serviceId) {
    PageRequest pageRequest = PageRequestBuilder.aPageRequest()
                                  .addFilter(InfrastructureDefinitionKeys.appId, Operator.EQ, appId)
                                  .addFilter(InfrastructureDefinitionKeys.envId, Operator.EQ, envId)
                                  .build();
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
    return list(pageRequest);
  }

  @Override
  public PageResponse<InfraDefinitionDetail> listInfraDefinitionDetail(
      PageRequest<InfrastructureDefinition> pageRequest, String appId, String envId) {
    PageResponse<InfrastructureDefinition> infrastructureDefinitionPageResponse = list(pageRequest);
    List<InfrastructureMapping> infrastructureMappings = infrastructureMappingService.listInfraMappings(appId, envId);
    CountingMap infraDefinitionIdMappingCount = new CountingMap();
    infrastructureMappings.forEach(infrastructureMapping
        -> infraDefinitionIdMappingCount.increment(infrastructureMapping.getInfrastructureDefinitionId()));
    List<InfraDefinitionDetail> infraDefinitionDetailList =
        infrastructureDefinitionPageResponse.getResponse()
            .stream()
            .map(infrastructureDefinition
                -> InfraDefinitionDetail.builder()
                       .infrastructureDefinition(infrastructureDefinition)
                       .countDerivedInfraMappings(
                           infraDefinitionIdMappingCount.count(infrastructureDefinition.getUuid()))
                       .build())
            .collect(Collectors.toList());
    return PageResponseBuilder.aPageResponse().withResponse(infraDefinitionDetailList).build();
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

  @VisibleForTesting
  public void validateInfraDefinition(@Valid InfrastructureDefinition infraDefinition) {
    if (isNotEmpty(infraDefinition.getProvisionerId())) {
      ProvisionerAware provisionerAwareInfra = (ProvisionerAware) infraDefinition.getInfrastructure();
      Map<String, String> expressions = provisionerAwareInfra.getExpressions();
      if (isEmpty(expressions)) {
        throw new InvalidRequestException("Expressions can't be empty with provisioner");
      }
      validateMandatoryFields(infraDefinition.getInfrastructure());
      removeUnsupportedExpressions(provisionerAwareInfra);
    }
    InfrastructureMapping infrastructureMapping = infraDefinition.getInfraMapping();
    // Some Hack To validate without Service Template
    infrastructureMapping.setServiceTemplateId("dummy");
    infrastructureMapping.setAccountId(appService.getAccountIdByAppId(infraDefinition.getAppId()));
    infrastructureMappingService.validateInfraMapping(infrastructureMapping, false);
  }

  @VisibleForTesting
  public void removeUnsupportedExpressions(ProvisionerAware provisionerAwareInfra) {
    Map<String, String> expressions = provisionerAwareInfra.getExpressions();
    Set<String> supportedExpressions = provisionerAwareInfra.getSupportedExpressions();

    if (supportedExpressions != null) {
      SetView<String> unsupportedExpressions = Sets.difference(expressions.keySet(), supportedExpressions);
      expressions.keySet().removeAll(new HashSet<>(unsupportedExpressions));
      provisionerAwareInfra.setExpressions(expressions);
    }
  }

  private void validateMandatoryFields(InfraMappingInfrastructureProvider infra) {
    if (infra instanceof AwsAmiInfrastructure) {
      validateAwsAmiInfraWithProvisioner((AwsAmiInfrastructure) infra);
    } else if (infra instanceof AwsInstanceInfrastructure) {
      validateAwsInstanceInfraWithProvisioner((AwsInstanceInfrastructure) infra);
    } else if (infra instanceof AwsLambdaInfrastructure) {
      validateAwsLambdaInfraWithProvisioner((AwsLambdaInfrastructure) infra);
    } else if (infra instanceof GoogleKubernetesEngine) {
      validateGoogleKubernetesEngineInfraWithProvisioner((GoogleKubernetesEngine) infra);
    } else if (infra instanceof PhysicalInfra) {
      validatePhysicalInfraWithProvisioner((PhysicalInfra) infra);
    } else if (infra instanceof AwsEcsInfrastructure) {
      validateAwsEcsInfraWithProvisioner((AwsEcsInfrastructure) infra);
    }
  }

  @VisibleForTesting
  public void validateAwsEcsInfraWithProvisioner(AwsEcsInfrastructure infra) {
    Map<String, String> expressions = infra.getExpressions();
    if (isEmpty(expressions.get(AwsEcsInfrastructureKeys.region))) {
      throw new InvalidRequestException("Region is required.");
    }
    if (isEmpty(expressions.get(AwsEcsInfrastructureKeys.clusterName))) {
      throw new InvalidRequestException("Cluster Name is required.");
    }
    if (isEmpty(infra.getLaunchType())) {
      throw new InvalidRequestException("Launch Type can't be empty");
    }
    if (StringUtils.equals(infra.getLaunchType(), LaunchType.FARGATE.toString())) {
      if (isEmpty(expressions.get(AwsEcsInfrastructureKeys.executionRole))) {
        throw new InvalidRequestException("execution role is required with Fargate Launch Type.");
      }
      if (isEmpty(expressions.get(AwsEcsInfrastructureKeys.vpcId))) {
        throw new InvalidRequestException("vpc-id is required with Fargate Launch Type.");
      }
      if (isEmpty(expressions.get(AwsEcsInfrastructureKeys.securityGroupIds))) {
        throw new InvalidRequestException("security-groupIds are required with Fargate Launch Type.");
      }
      if (isEmpty(expressions.get(AwsEcsInfrastructureKeys.subnetIds))) {
        throw new InvalidRequestException("subnet-ids are required with Fargate Launch Type.");
      }
    }
  }

  @VisibleForTesting
  public void validatePhysicalInfraWithProvisioner(PhysicalInfra infra) {
    Map<String, String> expressions = infra.getExpressions();
    if (isEmpty(expressions.get(PhysicalInfra.hostArrayPath))) {
      throw new InvalidRequestException("Host Array Path can't be empty");
    }
    if (isEmpty(expressions.get(PhysicalInfra.hostname))) {
      throw new InvalidRequestException("Hostname can't be empty");
    }
  }

  @VisibleForTesting
  public void validateGoogleKubernetesEngineInfraWithProvisioner(GoogleKubernetesEngine infra) {
    Map<String, String> expressions = infra.getExpressions();
    if (isEmpty(expressions.get(GoogleKubernetesEngineKeys.clusterName))) {
      throw new InvalidRequestException("Cluster name can't be empty");
    }
    if (isEmpty(expressions.get(GoogleKubernetesEngineKeys.namespace))) {
      throw new InvalidRequestException("Namespace can't be empty");
    }
  }

  @VisibleForTesting
  public void validateAwsLambdaInfraWithProvisioner(AwsLambdaInfrastructure infra) {
    Map<String, String> expressions = infra.getExpressions();
    if (isEmpty(expressions.get(AwsLambdaInfrastructureKeys.region))) {
      throw new InvalidRequestException("Region is mandatory");
    }
    if (StringUtils.isEmpty(expressions.get(AwsLambdaInfrastructureKeys.role))) {
      throw new InvalidRequestException("IAM Role is mandatory");
    }
  }

  @VisibleForTesting
  public void validateAwsInstanceInfraWithProvisioner(AwsInstanceInfrastructure infra) {
    Map<String, String> expressions = infra.getExpressions();
    if (infra.isProvisionInstances()) {
      if (isEmpty(expressions.get(AwsInstanceInfrastructureKeys.autoScalingGroupName))) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "Auto Scaling group Name must not be empty");
      }
      if (infra.isSetDesiredCapacity() && infra.getDesiredCapacity() <= 0) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "Desired count must be greater than zero.");
      }
    } else {
      if (isEmpty(expressions.get(AwsInstanceFilterKeys.tags))) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "Tags must not be empty with AWS Instance Filter");
      }
    }
  }

  @VisibleForTesting
  public void validateAwsAmiInfraWithProvisioner(AwsAmiInfrastructure infra) {
    Map<String, String> expressions = infra.getExpressions();
    if (isEmpty(expressions.get(AwsAmiInfrastructureKeys.region))) {
      throw new InvalidRequestException("Region is mandatory");
    }
    String baseAsgName = expressions.get(AwsAmiInfrastructureKeys.autoScalingGroupName);
    if (AmiDeploymentType.AWS_ASG.equals(infra.getAmiDeploymentType()) && isEmpty(baseAsgName)) {
      throw new InvalidRequestException("Auto Scaling Group is mandatory");
    }
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
    Validator.notNullCheck("Infrastructure definition", infrastructureDefinition);

    ensureSafeToDelete(appId, infrastructureDefinition.getUuid());

    wingsPersistence.delete(InfrastructureDefinition.class, appId, infraDefinitionId);
    yamlPushService.pushYamlChangeSet(accountId, infrastructureDefinition, null, Type.DELETE, false, false);
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
    if (context != null) {
      renderExpression(infrastructureDefinition, context);
    }
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
    Service service = serviceResourceService.get(infraDefinition.getAppId(), serviceId);
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

    // TODO => Hackish Find a better way to handle V1 services
    if (queryMap.containsKey("releaseName") && !service.isK8sV2()) {
      queryMap.remove("releaseName");
    }
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
          .field(InfrastructureDefinitionKeys.uuid)
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

  @Override
  public List<String> listHostDisplayNames(String appId, String infraDefinitionId, String workflowExecutionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infra definition was deleted", infrastructureDefinition, USER);
    return getInfrastructureMappingHostDisplayNames(infrastructureDefinition, appId, workflowExecutionId);
  }

  @Override
  public Map<String, String> listAwsIamRoles(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);

    return infrastructureMappingService.listAllRoles(
        appId, infrastructureDefinition.getInfrastructure().getCloudProviderId());
  }

  @Override
  public List<Host> listHosts(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);
    return listHosts(infrastructureDefinition);
  }

  @Override
  public List<AwsRoute53HostedZoneData> listHostedZones(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);

    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    SettingAttribute computeProviderSetting =
        settingsService.get(infrastructureDefinition.getInfrastructure().getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (provider instanceof AwsEcsInfrastructure) {
      String region = ((AwsEcsInfrastructure) provider).getRegion();
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, appId, null);
      return awsRoute53HelperServiceManager.listHostedZones(awsConfig, encryptionDetails, region, appId);
    }
    return emptyList();
  }

  @Override
  public List<Host> getAutoScaleGroupNodes(String appId, String infraDefinitionId, String workflowExecutionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("InfraStructure Definition Service", infrastructureDefinition);

    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    if (provider instanceof AwsInfrastructureMapping) {
      SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
      notNullCheck("Compute Provider", computeProviderSetting);

      AwsInfrastructureProvider awsInfrastructureProvider =
          (AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name());
      AwsInstanceInfrastructure awsInstanceInfrastructure = (AwsInstanceInfrastructure) provider;

      return awsInfrastructureProvider.maybeSetAutoScaleCapacityAndGetHosts(appId, workflowExecutionId,
          awsInstanceInfrastructure, computeProviderSetting, infrastructureDefinition.getEnvId(), infraDefinitionId);
    } else {
      throw new InvalidRequestException("Auto Scale groups are only supported for AWS infrastructure mapping");
    }
  }

  @Override
  public Map<String, String> listNetworkLoadBalancers(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);

    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();

    SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (provider instanceof AwsEcsInfrastructure || provider instanceof AwsInstanceInfrastructure) {
      String region = provider instanceof AwsEcsInfrastructure ? ((AwsEcsInfrastructure) provider).getRegion()
                                                               : ((AwsInstanceInfrastructure) provider).getRegion();

      return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
          .listNetworkBalancers(computeProviderSetting, region, appId)
          .stream()
          .collect(toMap(s -> s, s -> s));
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, String> listLoadBalancers(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);

    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (provider instanceof AwsInstanceInfrastructure || provider instanceof AwsEcsInfrastructure) {
      String region = provider instanceof AwsInstanceInfrastructure ? ((AwsInstanceInfrastructure) provider).getRegion()
                                                                    : ((AwsEcsInfrastructure) provider).getRegion();

      return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
          .listLoadBalancers(computeProviderSetting, region, appId)
          .stream()
          .collect(toMap(s -> s, s -> s));
    } else if (PHYSICAL_DATA_CENTER.name().equals(computeProviderSetting.getValue().getType())) {
      return settingsService
          .getGlobalSettingAttributesByType(computeProviderSetting.getAccountId(), SettingVariableTypes.ELB.name())
          .stream()
          .collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
    }
    return Collections.emptyMap();
  }

  @Override
  public List<AwsElbListener> listListeners(String appId, String infraDefinitionId, String loadbalancerName) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);
    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    String region = extractRegionFromInfrastructureProvider(provider);
    if (isEmpty(region)) {
      return Collections.emptyList();
    }
    SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);
    AwsInfrastructureProvider infrastructureProvider =
        (AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name());
    return infrastructureProvider.listListeners(computeProviderSetting, region, loadbalancerName, appId);
  }

  @Override
  public Map<String, String> listElasticLoadBalancers(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);
    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    String region = extractRegionFromInfrastructureProvider(provider);
    if (isEmpty(region)) {
      return Collections.emptyMap();
    }
    SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);
    return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
        .listElasticBalancers(computeProviderSetting, region, appId)
        .stream()
        .collect(toMap(s -> s, s -> s));
  }

  private String extractRegionFromInfrastructureProvider(InfraMappingInfrastructureProvider provider) {
    String region = EMPTY;
    if (provider instanceof AwsEcsInfrastructure) {
      region = ((AwsEcsInfrastructure) provider).getRegion();
    } else if (provider instanceof AwsInstanceInfrastructure) {
      region = ((AwsInstanceInfrastructure) provider).getRegion();
    } else if (provider instanceof AwsAmiInfrastructure) {
      region = ((AwsAmiInfrastructure) provider).getRegion();
    }
    return region;
  }

  @Override
  public Map<String, String> listTargetGroups(String appId, String infraDefinitionId, String loadbalancerName) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("InfraStructure Definition", infrastructureDefinition);

    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();

    SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (provider instanceof AwsInstanceInfrastructure || provider instanceof AwsEcsInfrastructure) {
      String region = provider instanceof AwsInstanceInfrastructure ? ((AwsInstanceInfrastructure) provider).getRegion()
                                                                    : ((AwsEcsInfrastructure) provider).getRegion();
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name());
      return infrastructureProvider.listTargetGroups(computeProviderSetting, region, loadbalancerName, appId);
    }
    return Collections.emptyMap();
  }

  @Override
  public String getContainerRunningInstances(
      String appId, String infraDefinitionId, String serviceId, String serviceNameExpression) {
    InfrastructureMapping infrastructureMapping = getInfraMapping(appId, infraDefinitionId, serviceId, null);
    return infrastructureMappingService.getContainerRunningInstances(
        appId, infrastructureMapping.getUuid(), serviceNameExpression);
  }

  private List<Host> listHosts(InfrastructureDefinition infrastructureDefinition) {
    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    if (provider instanceof PhysicalInfra) {
      PhysicalInfra physicalInfra = (PhysicalInfra) provider;
      if (EmptyPredicate.isNotEmpty(physicalInfra.getHosts())) {
        return physicalInfra.getHosts();
      }
      List<String> hostNames = physicalInfra.getHostNames()
                                   .stream()
                                   .map(String::trim)
                                   .filter(StringUtils::isNotEmpty)
                                   .distinct()
                                   .collect(toList());
      return hostNames.stream()
          .map(hostName
              -> aHost()
                     .withHostName(hostName)
                     .withPublicDns(hostName)
                     .withAppId(infrastructureDefinition.getAppId())
                     .withEnvId(infrastructureDefinition.getEnvId())
                     .withInfraDefinitionId(infrastructureDefinition.getUuid())
                     .withHostConnAttr(physicalInfra.getHostConnectionAttrs())
                     .build())
          .collect(toList());
    } else if (provider instanceof PhysicalInfraWinrm) {
      PhysicalInfraWinrm physicalInfraWinrm = (PhysicalInfraWinrm) provider;
      List<String> hostNames = physicalInfraWinrm.getHostNames()
                                   .stream()
                                   .map(String::trim)
                                   .filter(StringUtils::isNotEmpty)
                                   .distinct()
                                   .collect(toList());
      return hostNames.stream()
          .map(hostName
              -> aHost()
                     .withHostName(hostName)
                     .withPublicDns(hostName)
                     .withAppId(infrastructureDefinition.getAppId())
                     .withEnvId(infrastructureDefinition.getEnvId())
                     .withInfraDefinitionId(infrastructureDefinition.getUuid())
                     .withWinrmConnAttr(physicalInfraWinrm.getWinRmConnectionAttributes())
                     .build())
          .collect(toList());
    } else if (provider instanceof AwsInstanceInfrastructure) {
      AwsInstanceInfrastructure awsInstanceInfrastructure = (AwsInstanceInfrastructure) provider;
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name());
      SettingAttribute computeProviderSetting = settingsService.get(awsInstanceInfrastructure.getCloudProviderId());
      notNullCheck("Compute Provider", computeProviderSetting);
      return infrastructureProvider
          .listHosts(infrastructureDefinition, computeProviderSetting,
              secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null),
              new PageRequest<>())
          .getResponse();
    } else if (provider instanceof AzureInstanceInfrastructure) {
      AzureInstanceInfrastructure azureInstanceInfrastructure = (AzureInstanceInfrastructure) provider;
      SettingAttribute computeProviderSetting = settingsService.get(azureInstanceInfrastructure.getCloudProviderId());
      notNullCheck("Compute Provider", computeProviderSetting);

      return azureHelperService.listHosts(infrastructureDefinition, computeProviderSetting,
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null),
          infrastructureDefinition.getDeploymentType());
    } else {
      throw new InvalidRequestException("Unsupported cloud infrastructure: " + provider.getClass().getName());
    }
  }

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }
    return (AwsConfig) computeProviderSetting.getValue();
  }

  private List<String> getInfrastructureMappingHostDisplayNames(
      InfrastructureDefinition infrastructureDefinition, String appId, String workflowExecutionId) {
    List<String> hostDisplayNames = new ArrayList<>();
    if (infrastructureDefinition.getInfrastructure() instanceof PhysicalInfra) {
      PhysicalInfra physicalInfra = (PhysicalInfra) infrastructureDefinition.getInfrastructure();
      if (infrastructureDefinition.getProvisionerId() != null) {
        return physicalInfra.getHosts().stream().map(Host::getHostName).collect(Collectors.toList());
      }
      return physicalInfra.getHostNames();
    } else if (infrastructureDefinition.getInfrastructure() instanceof AwsInstanceInfrastructure) {
      AwsInstanceInfrastructure awsInfrastructureMapping =
          (AwsInstanceInfrastructure) infrastructureDefinition.getInfrastructure();
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name());
      SettingAttribute computeProviderSetting = settingsService.get(awsInfrastructureMapping.getCloudProviderId());
      notNullCheck("Compute Provider", computeProviderSetting);
      List<Host> hosts =
          infrastructureProvider
              .listHosts(infrastructureDefinition, computeProviderSetting,
                  secretManager.getEncryptionDetails(
                      (EncryptableSetting) computeProviderSetting.getValue(), appId, workflowExecutionId),
                  new PageRequest<>())
              .getResponse();
      for (Host host : hosts) {
        String displayName = host.getPublicDns();
        if (host.getEc2Instance() != null) {
          Optional<Tag> optNameTag =
              host.getEc2Instance().getTags().stream().filter(tag -> tag.getKey().equals("Name")).findFirst();
          if (optNameTag.isPresent() && isNotBlank(optNameTag.get().getValue())) {
            // UI checks for " [" in the name to get dns name only. If you change here then also update
            // NodeSelectModal.js
            displayName += " [" + optNameTag.get().getValue() + "]";
          }
        }
        hostDisplayNames.add(displayName);
      }
      return hostDisplayNames;
    } else if (infrastructureDefinition.getInfrastructure() instanceof AzureInstanceInfrastructure) {
      AzureInstanceInfrastructure azureInstanceInfrastructure =
          (AzureInstanceInfrastructure) infrastructureDefinition.getInfrastructure();
      SettingAttribute computeProviderSetting = settingsService.get(azureInstanceInfrastructure.getCloudProviderId());
      notNullCheck("Compute Provider", computeProviderSetting, USER);

      // Get VMs
      List<VirtualMachine> vms = azureHelperService.listVms(azureInstanceInfrastructure, computeProviderSetting,
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null),
          infrastructureDefinition.getDeploymentType());
      hostDisplayNames = vms.stream().map(vm -> vm.name()).collect(Collectors.toList());
      return hostDisplayNames;
    }
    return emptyList();
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

  private void prune(String appId, String infraDefinitionId) {
    pruneQueue.send(new PruneEvent(InfrastructureDefinition.class, appId, infraDefinitionId));
    wingsPersistence.delete(InfrastructureDefinition.class, appId, infraDefinitionId);
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    List<InfrastructureDefinition> infrastructureDefinitions =
        wingsPersistence.createQuery(InfrastructureDefinition.class)
            .filter(InfrastructureDefinitionKeys.appId, appId)
            .filter(InfrastructureDefinitionKeys.envId, envId)
            .project(InfrastructureDefinitionKeys.appId, true)
            .project(InfrastructureDefinitionKeys.envId, true)
            .project(InfrastructureDefinitionKeys.uuid, true)
            .asList();
    for (InfrastructureDefinition infrastructureDefinition : infrastructureDefinitions) {
      prune(appId, infrastructureDefinition.getUuid());
      auditServiceHelper.reportDeleteForAuditing(appId, infrastructureDefinition);
    }
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraDefinitionId){}

  ;

  @Override
  public void ensureSafeToDelete(@NotEmpty String appId, @NotEmpty String infraDefinitionId) {
    List<String> refWorkflows =
        workflowService.obtainWorkflowNamesReferencedByInfrastructureDefinition(appId, infraDefinitionId);

    if (!refWorkflows.isEmpty()) {
      throw new InvalidRequestException(
          format(" Infrastructure Definition %s is referenced by %s %s [%s].", infraDefinitionId, refWorkflows.size(),
              plural("workflow", refWorkflows.size()), Joiner.on(", ").join(refWorkflows)),
          USER);
    }

    List<String> refPipelines =
        pipelineService.obtainPipelineNamesReferencedByTemplatedEntity(appId, infraDefinitionId);
    if (isNotEmpty(refPipelines)) {
      throw new InvalidRequestException(
          format("Infrastructure Definition is referenced by %d %s [%s] as a workflow variable.", refPipelines.size(),
              plural("pipeline", refPipelines.size()), Joiner.on(", ").join(refPipelines)),
          USER);
    }

    List<String> refTriggers = triggerService.obtainTriggerNamesReferencedByTemplatedEntityId(appId, infraDefinitionId);
    if (isNotEmpty(refTriggers)) {
      throw new InvalidRequestException(
          format("Infrastructure Definition is referenced by %d %s [%s] as a workflow variable.", refTriggers.size(),
              plural("trigger", refTriggers.size()), Joiner.on(", ").join(refTriggers)),
          USER);
    }
  }

  @Override
  public List<String> listNamesByProvisionerId(@NotEmpty String appId, @NotEmpty String provisionerId) {
    return wingsPersistence.createQuery(InfrastructureDefinition.class)
        .filter(InfrastructureDefinitionKeys.appId, appId)
        .filter(InfrastructureDefinitionKeys.provisionerId, provisionerId)
        .project(InfrastructureDefinitionKeys.name, true)
        .asList()
        .stream()
        .map(InfrastructureDefinition::getName)
        .collect(toList());
  }

  @Override
  public List<String> listNamesByComputeProviderId(@NotEmpty String accountId, @NotEmpty String computeProviderId) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    return wingsPersistence.createQuery(InfrastructureDefinition.class)
        .field(InfrastructureDefinitionKeys.appId)
        .hasAnyOf(appIds)
        .project(InfrastructureDefinitionKeys.name, true)
        .project(InfrastructureDefinitionKeys.infrastructure, true)
        .asList()
        .stream()
        .filter(infrastructureDefinition
            -> infrastructureDefinition.getInfrastructure().getCloudProviderId().equals(computeProviderId))
        .map(InfrastructureDefinition::getName)
        .collect(toList());
  }

  @Override
  public List<String> listNamesByConnectionAttr(@NotEmpty String accountId, @NotEmpty String attributeId) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    List<InfrastructureDefinition> infrastructureDefinitions =
        wingsPersistence.createQuery(InfrastructureDefinition.class)
            .field(InfrastructureDefinitionKeys.appId)
            .hasAnyOf(appIds)
            .field(InfrastructureDefinitionKeys.deploymentType)
            .hasAnyOf(Arrays.asList(DeploymentType.SSH.name(), WINRM.name()))
            .project(InfrastructureDefinitionKeys.name, true)
            .project(InfrastructureDefinitionKeys.infrastructure, true)
            .asList();
    List<String> infraDefinitionNames = new ArrayList<>();
    for (InfrastructureDefinition infrastructureDefinition : CollectionUtils.emptyIfNull(infrastructureDefinitions)) {
      InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
      if (infrastructure instanceof SshBasedInfrastructure) {
        if (attributeId.equals(((SshBasedInfrastructure) infrastructure).getHostConnectionAttrs())) {
          infraDefinitionNames.add(infrastructureDefinition.getName());
        }
      }
      if (infrastructure instanceof WinRmBasedInfrastructure) {
        if (attributeId.equals(((WinRmBasedInfrastructure) infrastructure).getWinRmConnectionAttributes())) {
          infraDefinitionNames.add(infrastructureDefinition.getName());
        }
      }
    }
    return infraDefinitionNames;
  }

  public List<String> listNamesByScopedService(String appId, String serviceId) {
    return wingsPersistence.createQuery(InfrastructureDefinition.class)
        .filter(InfrastructureDefinitionKeys.appId, appId)
        .field(InfrastructureDefinitionKeys.scopedToServices)
        .hasThisOne(serviceId)
        .project(InfrastructureDefinitionKeys.name, true)
        .asList()
        .stream()
        .map(InfrastructureDefinition::getName)
        .collect(Collectors.toList());
  }
}
