package software.wings.service.impl.infrastructuredefinition;

import static io.harness.beans.PageRequest.PageRequestBuilder;
import static io.harness.beans.PageResponse.PageResponseBuilder;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ADMIN;
import static io.harness.exception.WingsException.USER;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_MAX_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_MIN_INSTANCES;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_NAME;
import static io.harness.spotinst.model.SpotInstConstants.DEFAULT_ELASTIGROUP_TARGET_INSTANCES;
import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.SPOTINST;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.api.DeploymentType.WINRM;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MAX_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MIN_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_NAME;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.Utils.safe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ecs.model.LaunchType;
import com.mongodb.DuplicateKeyException;
import io.fabric8.utils.CountingMap;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.delegate.task.spotinst.response.SpotinstElastigroupRunningCountData;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.FailureType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ReflectionException;
import io.harness.exception.WingsException;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.observer.Subject;
import io.harness.queue.QueuePublisher;
import io.harness.reflection.ReflectionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.PcfConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.infrastructure.Host;
import software.wings.common.InfrastructureConstants;
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
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.GoogleKubernetesEngine.GoogleKubernetesEngineKeys;
import software.wings.infra.InfraDefinitionDetail;
import software.wings.infra.InfraMappingDetail;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalDataCenterInfra;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.infra.ProvisionerAware;
import software.wings.infra.SshBasedInfrastructure;
import software.wings.infra.WinRmBasedInfrastructure;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.PcfHelperService;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;
import software.wings.service.impl.spotinst.SpotinstHelperServiceManager;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureDefinitionServiceObserver;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsRoute53HelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.utils.EcsConvention;
import software.wings.utils.Misc;
import software.wings.utils.ServiceVersionConvention;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class InfrastructureDefinitionServiceImpl implements InfrastructureDefinitionService {
  public static final String NULL = "null";
  public static final String DEFAULT = "default";
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private FeatureFlagService featureFlagService;
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
  @Inject private PcfHelperService pcfHelperService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private AwsAsgHelperServiceManager awsAsgHelperServiceManager;
  @Inject private SpotinstHelperServiceManager spotinstHelperServiceManager;
  @Inject private QueuePublisher<PruneEvent> pruneQueue;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private InfrastructureDefinitionHelper infrastructureDefinitionHelper;
  @Inject private EventPublishHelper eventPublishHelper;

  @Inject @Getter private Subject<InfrastructureDefinitionServiceObserver> subject = new Subject<>();

  private static Map<CloudProviderType, EnumSet<DeploymentType>> supportedCloudProviderDeploymentTypes =
      new EnumMap<>(CloudProviderType.class);
  static {
    supportedCloudProviderDeploymentTypes.put(
        CloudProviderType.AWS, EnumSet.of(SSH, WINRM, ECS, AWS_LAMBDA, AMI, AWS_CODEDEPLOY));
    supportedCloudProviderDeploymentTypes.put(CloudProviderType.AZURE, EnumSet.of(SSH, WINRM, HELM, KUBERNETES));
    supportedCloudProviderDeploymentTypes.put(CloudProviderType.GCP, EnumSet.of(HELM, KUBERNETES));
    supportedCloudProviderDeploymentTypes.put(CloudProviderType.KUBERNETES_CLUSTER, EnumSet.of(HELM, KUBERNETES));
    supportedCloudProviderDeploymentTypes.put(CloudProviderType.PCF, EnumSet.of(PCF));
    supportedCloudProviderDeploymentTypes.put(CloudProviderType.PHYSICAL_DATA_CENTER, EnumSet.of(SSH, WINRM));
  }

  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public PageResponse<InfrastructureDefinition> list(PageRequest<InfrastructureDefinition> pageRequest) {
    if (pageRequest.getUriInfo() != null) {
      if (pageRequest.getUriInfo().getQueryParameters().containsKey("serviceId")) {
        applyServiceFilter(pageRequest);
      }

      if (pageRequest.getUriInfo().getQueryParameters().containsKey("deploymentTypeFromMetadata")) {
        List<String> deploymentTypes = pageRequest.getUriInfo().getQueryParameters().get("deploymentTypeFromMetadata");
        pageRequest.addFilter(InfrastructureDefinitionKeys.deploymentType, Operator.IN, deploymentTypes.toArray());
      }
    }
    return wingsPersistence.query(InfrastructureDefinition.class, pageRequest);
  }

  @VisibleForTesting
  public void applyServiceFilter(PageRequest<InfrastructureDefinition> pageRequest) {
    if (!pageRequest.getUriInfo().getQueryParameters().containsKey("appId")) {
      throw new InvalidRequestException("AppId is mandatory for service-based filtering");
    }
    List<String> appIds = pageRequest.getUriInfo().getQueryParameters().get("appId");
    if (appIds.size() > 1) {
      throw new InvalidRequestException("More than 1 app not supported for listing infra definitions");
    }
    String appId = appIds.get(0);
    List<String> serviceIds = pageRequest.getUriInfo().getQueryParameters().get("serviceId");
    List<String> serviceNames = new ArrayList<>();
    EnumSet<DeploymentType> deploymentType = EnumSet.noneOf(DeploymentType.class);
    List<String> serviceIdsInScope = new ArrayList<>();
    for (String serviceId : serviceIds) {
      if (isEmpty(serviceId) || ExpressionEvaluator.matchesVariablePattern(serviceId)) {
        continue;
      }
      Service service = serviceResourceService.get(appId, serviceId);
      if (service == null) {
        throw new InvalidRequestException(format("No service exists for id : [%s]", serviceId));
      }
      if (service.getDeploymentType() != null) {
        // get deployment type array from filter
        deploymentType.add(service.getDeploymentType());
        serviceNames.add(service.getName());
      }
      serviceIdsInScope.add(serviceId);
    }

    if (isNotEmpty(deploymentType)) {
      if (deploymentType.size() > 1) {
        throw new InvalidRequestException(
            "Cannot load infra for different deployment type services " + serviceNames, USER);
      }
      if (isEmpty(pageRequest.getUriInfo().getQueryParameters().get("deploymentTypeFromMetadata"))) {
        pageRequest.addFilter(
            InfrastructureDefinitionKeys.deploymentType, Operator.EQ, deploymentType.iterator().next().name());
      }
    }

    if (isNotEmpty(serviceIdsInScope)) {
      SearchFilter op1 = SearchFilter.builder()
                             .fieldName(InfrastructureDefinitionKeys.scopedToServices)
                             .op(Operator.NOT_EXISTS)
                             .build();

      SearchFilter op2 = SearchFilter.builder()
                             .fieldName(InfrastructureDefinitionKeys.scopedToServices)
                             .op(Operator.CONTAINS)
                             .fieldValues(serviceIdsInScope.toArray())
                             .build();
      pageRequest.addFilter(InfrastructureDefinitionKeys.scopedToServices, Operator.OR, op1, op2);
    }
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
  public InfrastructureDefinition save(InfrastructureDefinition infrastructureDefinition, boolean migration) {
    return save(infrastructureDefinition, migration, false);
  }

  @Override
  public InfrastructureDefinition save(
      InfrastructureDefinition infrastructureDefinition, boolean migration, boolean skipValidation) {
    String accountId = appService.getAccountIdByAppId(infrastructureDefinition.getAppId());
    setMissingValues(infrastructureDefinition);

    if (!migration && !skipValidation) {
      validateInfraDefinition(infrastructureDefinition);
    }
    String uuid;
    try {
      uuid = wingsPersistence.save(infrastructureDefinition);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException(
          format("Infra definition already exists with the name : [%s]", infrastructureDefinition.getName()),
          WingsException.USER);
    }
    infrastructureDefinition.setUuid(uuid);
    if (!migration) {
      yamlPushService.pushYamlChangeSet(accountId, null, infrastructureDefinition, Type.CREATE, false, false);
    }
    if (!infrastructureDefinition.isSample()) {
      eventPublishHelper.publishAccountEvent(accountId,
          AccountEvent.builder().accountEventType(AccountEventType.INFRA_DEFINITION_ADDED).build(), true, true);
    }

    try {
      subject.fireInform(InfrastructureDefinitionServiceObxxxxxxxx:onSaved, infrastructureDefinition);
    } catch (Exception e) {
      logger.error("Encountered exception while informing the observers of Infrastructure Mappings.", e);
    }
    return infrastructureDefinition;
  }

  @VisibleForTesting
  void setMissingValues(InfrastructureDefinition infraDefinition) {
    infraDefinition.setAccountId(appService.getAccountIdByAppId(infraDefinition.getAppId()));
    if (infraDefinition.getInfrastructure() instanceof GoogleKubernetesEngine) {
      GoogleKubernetesEngine googleKubernetesEngine = (GoogleKubernetesEngine) infraDefinition.getInfrastructure();
      if (isBlank(googleKubernetesEngine.getNamespace())) {
        googleKubernetesEngine.setNamespace(DEFAULT);
      }
    }
    if (infraDefinition.getInfrastructure() instanceof DirectKubernetesInfrastructure) {
      DirectKubernetesInfrastructure directKubernetesInfrastructure =
          (DirectKubernetesInfrastructure) infraDefinition.getInfrastructure();
      if (isBlank(directKubernetesInfrastructure.getNamespace())) {
        directKubernetesInfrastructure.setNamespace(DEFAULT);
      }
    }
  }

  @VisibleForTesting
  public void validateInfraDefinition(@Valid InfrastructureDefinition infraDefinition) {
    validateCloudProviderAndDeploymentType(infraDefinition.getCloudProviderType(), infraDefinition.getDeploymentType());
    if (infraDefinition.getDeploymentType() == DeploymentType.SSH
        && infraDefinition.getInfrastructure() instanceof SshBasedInfrastructure) {
      notEmptyCheck("Connection Attributes can't be empty",
          ((SshBasedInfrastructure) infraDefinition.getInfrastructure()).getHostConnectionAttrs());
    }
    if (infraDefinition.getDeploymentType() == DeploymentType.WINRM
        && infraDefinition.getInfrastructure() instanceof WinRmBasedInfrastructure) {
      notEmptyCheck("Connection Attributes can't be empty",
          ((WinRmBasedInfrastructure) infraDefinition.getInfrastructure()).getWinRmConnectionAttributes());
    }
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
    infrastructureMapping.setAccountId(appService.getAccountIdByAppId(infraDefinition.getAppId()));
    infrastructureMappingService.validateInfraMapping(infrastructureMapping, false);
  }

  private void validateCloudProviderAndDeploymentType(
      CloudProviderType cloudProviderType, DeploymentType deploymentType) {
    if (supportedCloudProviderDeploymentTypes.containsKey(cloudProviderType)) {
      if (supportedCloudProviderDeploymentTypes.get(cloudProviderType).contains(deploymentType)) {
        return;
      }
    }
    throw new InvalidRequestException("Invalid CloudProvider and Deployment type combination", USER);
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
        throw new InvalidArgumentsException(Pair.of("args", "Auto Scaling group Name must not be empty"));
      }
      if (infra.isSetDesiredCapacity() && infra.getDesiredCapacity() <= 0) {
        throw new InvalidArgumentsException(Pair.of("args", "Desired count must be greater than zero."));
      }
    } else {
      if (isEmpty(expressions.get(AwsInstanceFilterKeys.tags))) {
        throw new InvalidArgumentsException(Pair.of("args", "Tags must not be empty with AWS Instance Filter"));
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
    if (AmiDeploymentType.AWS_ASG == infra.getAmiDeploymentType() && isEmpty(baseAsgName)) {
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
    setMissingValues(infrastructureDefinition);
    validateInfraDefinition(infrastructureDefinition);
    InfrastructureDefinition savedInfraDefinition =
        get(infrastructureDefinition.getAppId(), infrastructureDefinition.getUuid());
    if (savedInfraDefinition == null) {
      throw new InvalidRequestException(
          format("Infra Definition does not exist with id: [%s]", infrastructureDefinition.getUuid()));
    }
    validateImmutableFields(infrastructureDefinition, savedInfraDefinition);
    infrastructureDefinition.setCreatedAt(savedInfraDefinition.getCreatedAt());
    infrastructureDefinition.setCreatedBy(savedInfraDefinition.getCreatedBy());
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

    try {
      subject.fireInform(InfrastructureDefinitionServiceObxxxxxxxx:onUpdated, infrastructureDefinition);
    } catch (Exception e) {
      logger.error("Encountered exception while informing the observers of Infrastructure Mappings.", e);
    }
    return infrastructureDefinition;
  }

  public void validateImmutableFields(
      InfrastructureDefinition newInfrastructureDefinition, InfrastructureDefinition oldInfraDefinition) {
    if (oldInfraDefinition.getDeploymentType() != newInfrastructureDefinition.getDeploymentType()) {
      throw new InvalidRequestException("Deployment Type is immutable");
    }
    if (oldInfraDefinition.getCloudProviderType() != newInfrastructureDefinition.getCloudProviderType()) {
      throw new InvalidRequestException("Cloud Provider Type is immutable");
    }
    if (newInfrastructureDefinition.getInfrastructure() instanceof AwsAmiInfrastructure) {
      AwsAmiInfrastructure oldInfra = (AwsAmiInfrastructure) oldInfraDefinition.getInfrastructure();
      AwsAmiInfrastructure newInfra = (AwsAmiInfrastructure) newInfrastructureDefinition.getInfrastructure();
      if (oldInfra.isAsgIdentifiesWorkload() != newInfra.isAsgIdentifiesWorkload()) {
        throw new InvalidRequestException("\"Asg Uniquely Identifies Workload\" field is immutable");
      }
      if (oldInfra.isUseTrafficShift() != newInfra.isUseTrafficShift()) {
        throw new InvalidRequestException("\"Use Traffic Shift\" field is immutable");
      }
    }
  }

  @Override
  public void delete(String appId, String infraDefinitionId) {
    String accountId = appService.getAccountIdByAppId(appId);
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure definition", infrastructureDefinition);

    ensureSafeToDelete(appId, infrastructureDefinition);

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
      if (!renderExpression(infrastructureDefinition, context)) {
        return null;
      }
    }

    /*
     * setDefaults is called as the namespace is empty
     * for already saved infraDef, could remove setDef() after DB migration
     * */
    setMissingValues(infrastructureDefinition);
    return getInfrastructureMapping(serviceId, infrastructureDefinition);
  }

  @Override
  public InfrastructureMapping getInfrastructureMapping(
      String serviceId, InfrastructureDefinition infrastructureDefinition) {
    Service service = serviceResourceService.get(infrastructureDefinition.getAppId(), serviceId);
    InfrastructureMapping newInfraMapping = infrastructureDefinition.getInfraMapping();
    InfrastructureMapping infraMapping =
        infrastructureDefinitionHelper.existingInfraMapping(infrastructureDefinition, serviceId);
    newInfraMapping.setServiceId(serviceId);
    newInfraMapping.setAccountId(appService.getAccountIdByAppId(infrastructureDefinition.getAppId()));
    newInfraMapping.setInfrastructureDefinitionId(infrastructureDefinition.getUuid());
    newInfraMapping.setAutoPopulate(false);
    newInfraMapping.setDisplayName(format("%s-%s", service.getName(), infrastructureDefinition.getName()));
    newInfraMapping.setName(
        infrastructureDefinitionHelper.getNameFromInfraDefinition(infrastructureDefinition, serviceId));
    if (infraMapping == null) {
      try {
        return infrastructureMappingService.save(newInfraMapping, true);
      } catch (DuplicateFieldException ex) {
        logger.info("Trying to save but Existing InfraMapping Found. Updating........");
        infraMapping = infrastructureDefinitionHelper.existingInfraMapping(infrastructureDefinition, serviceId);
        newInfraMapping.setUuid(infraMapping.getUuid());
        newInfraMapping.setName(infraMapping.getName());
        return infrastructureMappingService.update(newInfraMapping, true);
      }
    } else {
      logger.info("Existing InfraMapping Found Updating.....");
      newInfraMapping.setUuid(infraMapping.getUuid());
      newInfraMapping.setName(infraMapping.getName());
      return infrastructureMappingService.update(newInfraMapping, true);
    }
  }

  boolean renderExpression(InfrastructureDefinition infrastructureDefinition, ExecutionContext context) {
    Set<String> ignoredExpressions = ImmutableSet.of(InfrastructureConstants.INFRA_KUBERNETES_INFRAID_EXPRESSION);
    Map<String, Object> fieldMapForClass = getExpressionAnnotatedFields(infrastructureDefinition.getInfrastructure());
    Map<String, String> renderedFieldMap = new HashMap<>();

    if (isEmpty(infrastructureDefinition.getProvisionerId())) {
      for (Entry<String, Object> entry : fieldMapForClass.entrySet()) {
        if (entry.getValue() instanceof String
            && ExpressionEvaluator.containsVariablePattern((String) entry.getValue())) {
          String expression = (String) entry.getValue();
          String renderedValue = context.renderExpression(expression);
          if ((expression.equals(renderedValue) || renderedValue == null || NULL.equals(renderedValue))
              && !isIgnored(ignoredExpressions, expression)) {
            throw new InvalidRequestException(format("Unable to resolve expression : \"%s\"", expression), USER);
          }
          renderedFieldMap.put(entry.getKey(), renderedValue);
        }
      }
      saveFieldMapForDefinition(infrastructureDefinition, renderedFieldMap);
    } else {
      ProvisionerAware provisionerAwareInfrastructure = (ProvisionerAware) infrastructureDefinition.getInfrastructure();
      InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerService.get(
          infrastructureDefinition.getAppId(), infrastructureDefinition.getProvisionerId());

      Map<String, Object> resolvedExpressions = infrastructureProvisionerService.resolveExpressions(
          infrastructureDefinition, context.asMap(), infrastructureProvisioner);
      provisionerAwareInfrastructure.applyExpressions(resolvedExpressions, infrastructureDefinition.getAppId(),
          infrastructureDefinition.getEnvId(), infrastructureDefinition.getUuid());
    }
    return true;
  }

  private boolean isIgnored(Set<String> ignoredExpressions, String expression) {
    return ignoredExpressions.stream().anyMatch(expression::contains);
  }

  @VisibleForTesting
  public Map<String, Object> getExpressionAnnotatedFields(InfraMappingInfrastructureProvider infrastructure) {
    // TODO: we should reconsider using reflection for this goal
    Map<String, Object> fieldValueMap = new HashMap<>();
    List<Field> declaredFields = ReflectionUtils.getDeclaredAndInheritedFields(
        infrastructure.getClass(), field -> field.getAnnotation(Expression.class) != null);
    for (Field declaredField : declaredFields) {
      if ("$jacocoData".equals(declaredField.getName())) {
        continue;
      }
      try {
        declaredField.setAccessible(true);
        fieldValueMap.put(declaredField.getName(), declaredField.get(infrastructure));
      } catch (IllegalAccessException e) {
        throw new ReflectionException("Illegal access for field", e, ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION,
            Level.ERROR, ADMIN, EnumSet.of(FailureType.APPLICATION_ERROR));
      }
    }
    return fieldValueMap;
  }

  private void saveFieldMapForDefinition(
      InfrastructureDefinition infrastructureDefinition, Map<String, String> fieldMapForClass) {
    try {
      Class<? extends InfraMappingInfrastructureProvider> aClass =
          infrastructureDefinition.getInfrastructure().getClass();
      for (Entry<String, String> entry : fieldMapForClass.entrySet()) {
        Field field = aClass.getDeclaredField(entry.getKey());
        field.setAccessible(true);
        field.set(infrastructureDefinition.getInfrastructure(), entry.getValue());
      }
    } catch (Exception ex) {
      throw new InvalidArgumentsException(Pair.of("message", ExceptionUtils.getMessage(ex)));
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
        .filter(InfrastructureDefinitionKeys.appId, appId)
        .filter(InfrastructureDefinitionKeys.envId, envId)
        .filter(InfrastructureDefinitionKeys.name, infraDefName)
        .get();
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
    infraDefinitionDetail.setDerivedInfraMappingDetailList(
        safe(getMappings(infrastructureDefinition.getUuid(), appId))
            .parallelStream()
            .map(infraMapping
                -> InfraMappingDetail.builder()
                       .infrastructureMapping(infraMapping)
                       .workflowExecutionList(getLatestWFEFor(infraMapping.getAppId(), infraMapping.getUuid(), 1))
                       .build())
            .collect(toList()));

    return infraDefinitionDetail;
  }

  private List<WorkflowExecution> getLatestWFEFor(String appId, String infraMappingId, int limit) {
    try {
      final ImmutableList<String> fieldList = ImmutableList.of(WorkflowExecutionKeys.appId,
          WorkflowExecutionKeys.createdAt, WorkflowExecutionKeys.accountId, WorkflowExecutionKeys.envId,
          WorkflowExecutionKeys.envIds, WorkflowExecutionKeys.infraDefinitionIds, WorkflowExecutionKeys.infraMappingIds,
          WorkflowExecutionKeys.appName, WorkflowExecutionKeys.envName, WorkflowExecutionKeys.envType,
          WorkflowExecutionKeys.workflowType, WorkflowExecutionKeys.status, WorkflowExecutionKeys.pipelineExecutionId,
          WorkflowExecutionKeys.name, WorkflowExecutionKeys.triggeredBy, WorkflowExecutionKeys.orchestrationType,
          WorkflowExecutionKeys.artifacts);

      return workflowExecutionService.getLatestExecutionsFor(appId, infraMappingId, limit, fieldList, true);

    } catch (Exception e) {
      logger.error("Failed to fetch recent executions for inframapping [{}]", infraMappingId, e);
    }
    return Collections.emptyList();
  }

  @Override
  public List<String> listHostDisplayNames(String appId, String infraDefinitionId, String workflowExecutionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infra definition was deleted", infrastructureDefinition, USER);
    return getInfrastructureDefinitionHostDisplayNames(infrastructureDefinition, appId, workflowExecutionId);
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

    String region = extractRegionFromInfrastructureProvider(provider);
    if (isBlank(region)) {
      return Collections.emptyMap();
    }

    return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
        .listNetworkBalancers(computeProviderSetting, region, appId)
        .stream()
        .collect(toMap(identity(), identity()));
  }

  @Override
  public List<AwsLoadBalancerDetails> listNetworkLoadBalancerDetails(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);

    InfraMappingInfrastructureProvider infrastructureProvider = infrastructureDefinition.getInfrastructure();

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureProvider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    String region = extractRegionFromInfrastructureProvider(infrastructureProvider);
    if (isBlank(region)) {
      return Collections.EMPTY_LIST;
    }

    return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
        .listNetworkLoadBalancerDetails(computeProviderSetting, region, appId);
  }

  @Override
  public Map<String, String> listLoadBalancers(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition", infrastructureDefinition);

    InfraMappingInfrastructureProvider provider = infrastructureDefinition.getInfrastructure();
    SettingAttribute computeProviderSetting = settingsService.get(provider.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (provider instanceof AwsInstanceInfrastructure || provider instanceof AwsEcsInfrastructure
        || provider instanceof AwsAmiInfrastructure) {
      String region = extractRegionFromInfrastructureProvider(provider);
      if (isBlank(region)) {
        return Collections.EMPTY_MAP;
      }

      return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
          .listLoadBalancers(computeProviderSetting, region, appId)
          .stream()
          .collect(toMap(identity(), identity()));
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
    notNullCheck("ComputeProvider", computeProviderSetting);
    return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
        .listElasticBalancers(computeProviderSetting, region, appId)
        .stream()
        .collect(toMap(identity(), identity()));
  }

  @Override
  public List<AwsLoadBalancerDetails> listElasticLoadBalancerDetails(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("InfraDefinition", infrastructureDefinition);
    InfraMappingInfrastructureProvider infrastructureProvider = infrastructureDefinition.getInfrastructure();
    String region = extractRegionFromInfrastructureProvider(infrastructureProvider);
    if (isEmpty(region)) {
      return Collections.EMPTY_LIST;
    }
    SettingAttribute computeProviderSetting = settingsService.get(infrastructureProvider.getCloudProviderId());
    notNullCheck("ComputeProvider", computeProviderSetting);
    return ((AwsInfrastructureProvider) infrastructureProviderMap.get(AWS.name()))
        .listElasticLoadBalancerDetails(computeProviderSetting, region, appId);
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
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infra Definition not found", infrastructureDefinition);
    if (isNotEmpty(infrastructureDefinition.getProvisionerId())) {
      return "0";
    }
    InfrastructureMapping infrastructureMapping = infrastructureDefinition.getInfraMapping();
    infrastructureMapping.setServiceId(serviceId);
    return infrastructureMappingService.getContainerRunningInstances(serviceNameExpression, infrastructureMapping);
  }

  List<Host> listHosts(InfrastructureDefinition infrastructureDefinition) {
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
      throw new InvalidArgumentsException(Pair.of("args", "InvalidConfiguration"));
    }
    return (AwsConfig) computeProviderSetting.getValue();
  }

  List<String> getInfrastructureDefinitionHostDisplayNames(
      InfrastructureDefinition infrastructureDefinition, String appId, String workflowExecutionId) {
    if (infrastructureDefinition.getProvisionerId() != null) {
      return emptyList();
    }
    List<String> hostDisplayNames = new ArrayList<>();
    if (infrastructureDefinition.getInfrastructure() instanceof PhysicalDataCenterInfra) {
      PhysicalDataCenterInfra physicalInfra = (PhysicalDataCenterInfra) infrastructureDefinition.getInfrastructure();
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
      List<Host> hosts = listHosts(infrastructureDefinition);
      if (isNotEmpty(hosts)) {
        hosts.forEach(host -> {
          String hostname = isEmpty(host.getPublicDns()) ? host.getHostName() : host.getPublicDns();
          hostDisplayNames.add(hostname);
        });
      }
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
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraDefinitionId) {
    // no descending entity for infra definition
  }

  @Override
  public void ensureSafeToDelete(@NotEmpty String appId, InfrastructureDefinition infrastructureDefinition) {
    final String infraDefinitionId = infrastructureDefinition.getUuid();
    final String infraDefinitionName = infrastructureDefinition.getName();
    List<String> refWorkflows =
        workflowService.obtainWorkflowNamesReferencedByInfrastructureDefinition(appId, infraDefinitionId);

    if (!refWorkflows.isEmpty()) {
      throw new InvalidRequestException(
          format(" Infrastructure Definition %s is referenced by %s %s [%s]", infraDefinitionName, refWorkflows.size(),
              plural("workflow", refWorkflows.size()), HarnessStringUtils.join(", ", refWorkflows)),
          USER);
    }

    List<String> refPipelines =
        pipelineService.obtainPipelineNamesReferencedByTemplatedEntity(appId, infraDefinitionId);
    if (isNotEmpty(refPipelines)) {
      throw new InvalidRequestException(
          format("Infrastructure Definition %s is referenced by %d %s [%s] as a workflow variable", infraDefinitionName,
              refPipelines.size(), plural("pipeline", refPipelines.size()),
              HarnessStringUtils.join(", ", refPipelines)),
          USER);
    }

    List<String> refTriggers = triggerService.obtainTriggerNamesReferencedByTemplatedEntityId(appId, infraDefinitionId);
    if (isNotEmpty(refTriggers)) {
      throw new InvalidRequestException(
          format("Infrastructure Definition %s is referenced by %d %s [%s] as a workflow variable", infraDefinitionName,
              refTriggers.size(), plural("trigger", refTriggers.size()), HarnessStringUtils.join(", ", refTriggers)),
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

  @Override
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

  @Override
  public void cloneInfrastructureDefinitions(
      final String sourceAppID, final String sourceEnvID, final String targetAppID, final String targetEnvID) {
    safe(list(sourceAppID, sourceEnvID, null)).forEach(infraDef -> {
      try {
        final InfrastructureDefinition clonedInfraDef = infraDef.cloneForUpdate();
        clonedInfraDef.setEnvId(targetEnvID);
        clonedInfraDef.setAppId(targetAppID);
        save(clonedInfraDef, false, true);
      } catch (Exception e) {
        logger.error("Failed to clone infrastructure definition name {}, id {} of environment {}", infraDef.getName(),
            infraDef.getUuid(), infraDef.getEnvId(), e);
      }
    });
  }

  @Override
  public List<InfrastructureDefinition> getNameAndIdForEnvironments(String appId, List<String> envIds) {
    return getDefinitionWithFieldsForEnvironments(appId, envIds, null);
  }

  @Override
  public List<InfrastructureDefinition> getNameAndIdForEnvironment(String appId, String envId, int limit) {
    Query<InfrastructureDefinition> infrastructureDefinitionQuery =
        wingsPersistence.createQuery(InfrastructureDefinition.class)
            .project(InfrastructureDefinitionKeys.uuid, true)
            .project(InfrastructureDefinitionKeys.name, true)
            .filter(InfrastructureDefinitionKeys.appId, appId)
            .filter(InfrastructureDefinitionKeys.envId, envId);
    FindOptions findOptions = new FindOptions();
    findOptions.limit(limit);
    return infrastructureDefinitionQuery.asList(findOptions);
  }

  @Override
  public Map<String, Integer> getCountForEnvironments(String appId, @NotNull List<String> envIds) {
    Map<String, Integer> envIdInfraDefCountMap =
        envIds.stream().collect(Collectors.toMap(envId -> envId, envId -> 0, (a, b) -> b));

    Query<InfrastructureDefinition> query = wingsPersistence.createQuery(InfrastructureDefinition.class)
                                                .project(InfrastructureDefinitionKeys.uuid, true)
                                                .project(InfrastructureDefinitionKeys.envId, true)
                                                .project(InfrastructureDefinitionKeys.name, true)
                                                .filter(InfrastructureDefinitionKeys.appId, appId)
                                                .field(InfrastructureDefinitionKeys.envId)
                                                .in(envIds);
    wingsPersistence.getDatastore(InfrastructureDefinition.class)
        .createAggregation(InfrastructureDefinition.class)
        .match(query)
        .group(Group.id(grouping("envId")), grouping("count", accumulator("$sum", 1)))
        .project(projection("envId", "_id.envId"), projection("count"))
        .aggregate(EnvInfraDefStats.class)
        .forEachRemaining(envInfraDefStat -> envIdInfraDefCountMap.put(envInfraDefStat.envId, envInfraDefStat.count));

    return envIdInfraDefCountMap;
  }

  @Data
  @NoArgsConstructor
  private static class EnvInfraDefStats {
    private String envId;
    private int count;
  }

  @Override
  public List<InfrastructureDefinition> getDefinitionWithFieldsForEnvironments(
      String appId, List<String> envIds, List<String> projections) {
    Query baseQuery = wingsPersistence.createQuery(InfrastructureDefinition.class)
                          .project(InfrastructureDefinitionKeys.uuid, true)
                          .project(InfrastructureDefinitionKeys.name, true)
                          .filter(InfrastructureDefinitionKeys.appId, appId)
                          .field(InfrastructureDefinitionKeys.envId)
                          .in(envIds);

    if (EmptyPredicate.isNotEmpty(projections)) {
      projections.forEach(projection -> baseQuery.project(projection, true));
    }
    return baseQuery.asList();
  }

  @Override
  public AwsAsgGetRunningCountData getAmiCurrentlyRunningInstanceCount(
      String appId, String infraDefinitionId, String serviceId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("InfraStructure Definition", infrastructureDefinition);
    if (isNotEmpty(infrastructureDefinition.getProvisionerId())) {
      // case that could happen since we support dynamic infra for Ami Asg
      return AwsAsgGetRunningCountData.builder()
          .asgName(DEFAULT_AMI_ASG_NAME)
          .asgMin(DEFAULT_AMI_ASG_MIN_INSTANCES)
          .asgMax(DEFAULT_AMI_ASG_MAX_INSTANCES)
          .asgDesired(DEFAULT_AMI_ASG_DESIRED_INSTANCES)
          .build();
    }
    AwsAmiInfrastructure awsAmiInfrastructure = (AwsAmiInfrastructure) infrastructureDefinition.getInfrastructure();
    SettingAttribute computeProviderSetting = settingsService.get(awsAmiInfrastructure.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);
    String region = awsAmiInfrastructure.getRegion();
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, appId, null);

    InfrastructureMapping infrastructureMapping =
        infrastructureDefinitionService.getInfraMapping(appId, serviceId, infraDefinitionId, null);
    notNullCheck("Infrastructure Mapping does not exist", infrastructureDefinition);
    return awsAsgHelperServiceManager.getCurrentlyRunningInstanceCount(
        awsConfig, encryptionDetails, region, infrastructureMapping.getUuid(), appId);
  }

  @Override
  public Integer getPcfRunningInstances(
      String appId, String infraDefinitionId, String appNameExpression, String serviceId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition does not exist", infrastructureDefinition);

    Application app = appService.get(infrastructureDefinition.getAppId());
    Environment env =
        environmentService.get(infrastructureDefinition.getAppId(), infrastructureDefinition.getEnvId(), false);
    Service service = serviceResourceService.get(appId, serviceId);

    Map<String, Object> context = new HashMap<>();
    context.put("app", app);
    context.put("env", env);
    context.put("service", service);

    InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
    if (!(infrastructure instanceof PcfInfraStructure)) {
      throw new InvalidArgumentsException(Pair.of("args", "InvalidConfiguration, Needs Instance of PcfConfig"));
    }

    SettingAttribute computeProviderSetting = settingsService.get(infrastructure.getCloudProviderId());
    notNullCheck("Compute Provider Does not Exist", computeProviderSetting);

    appNameExpression = StringUtils.isNotBlank(appNameExpression)
        ? Misc.normalizeExpression(evaluator.substitute(appNameExpression, context))
        : EcsConvention.getTaskFamily(app.getName(), service.getName(), env.getName());

    return pcfHelperService.getRunningInstanceCount((PcfConfig) computeProviderSetting.getValue(),
        ((PcfInfraStructure) infrastructure).getOrganization(), ((PcfInfraStructure) infrastructure).getSpace(),
        appNameExpression);
  }

  @Override
  public List<ElastiGroup> listElastiGroups(String appId, String computeProviderId) {
    SpotInstConfig spotInstConfig = validateAndGetSpotinstConfig(computeProviderId);
    try {
      return spotinstHelperServiceManager.listElastigroups(
          spotInstConfig, secretManager.getEncryptionDetails(spotInstConfig, appId, null), appId);
    } catch (Exception e) {
      logger.warn(ExceptionUtils.getMessage(e), e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  @Override
  public String getElastigroupJson(String appId, String computeProviderId, String elastigroupId) {
    SpotInstConfig spotInstConfig = validateAndGetSpotinstConfig(computeProviderId);
    try {
      return spotinstHelperServiceManager.getElastigroupJson(
          spotInstConfig, secretManager.getEncryptionDetails(spotInstConfig, appId, null), appId, elastigroupId);
    } catch (Exception e) {
      logger.warn(ExceptionUtils.getMessage(e), e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  @Override
  public SpotinstElastigroupRunningCountData getElastigroupRunningCountData(
      String appId, String infraDefinitionId, String elastigroupNameExpression, String serviceId, boolean blueGreen) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition does not exist", infrastructureDefinition);

    InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
    if (!(infrastructure instanceof AwsAmiInfrastructure)) {
      throw new InvalidArgumentsException(
          Pair.of("args", "InvalidConfiguration, Needs Instance of Aws Spotinst config"));
    }

    List<ElastiGroup> groups =
        listElastiGroups(appId, ((AwsAmiInfrastructure) infrastructure).getSpotinstCloudProvider());
    if (isEmpty(groups)) {
      return SpotinstElastigroupRunningCountData.builder()
          .elastigroupMin(DEFAULT_ELASTIGROUP_MIN_INSTANCES)
          .elastigroupMax(DEFAULT_ELASTIGROUP_MAX_INSTANCES)
          .elastigroupTarget(DEFAULT_ELASTIGROUP_TARGET_INSTANCES)
          .elastigroupName(DEFAULT_ELASTIGROUP_NAME)
          .build();
    }

    Application app = appService.get(infrastructureDefinition.getAppId());
    Environment env =
        environmentService.get(infrastructureDefinition.getAppId(), infrastructureDefinition.getEnvId(), false);
    Service service = serviceResourceService.get(appId, serviceId);

    String elastigroupName;
    if (isEmpty(elastigroupNameExpression)) {
      elastigroupName = ServiceVersionConvention.getPrefix(app.getName(), service.getName(), env.getName());
    } else {
      Map<String, Object> context = new HashMap<>();
      context.put("app", app);
      context.put("env", env);
      context.put("service", service);
      elastigroupName = evaluator.substitute(elastigroupNameExpression, context);
    }
    String finalElastigroupName = Misc.normalizeExpression(elastigroupName);

    Optional<ElastiGroup> group;
    if (blueGreen) {
      group = groups.stream().filter(g -> finalElastigroupName.equals(g.getName())).findFirst();
    } else {
      String prefix = format("%s__", finalElastigroupName);
      List<ElastiGroup> groupsWithNames =
          groups.stream()
              .filter(item -> {
                String name = item.getName();
                if (!name.startsWith(prefix)) {
                  return false;
                }
                String temp = name.substring(prefix.length());
                return temp.matches("[0-9]+");
              })
              .sorted(Comparator.comparingInt(g -> Integer.parseInt(g.getName().substring(prefix.length()))))
              .collect(toList());
      if (isEmpty(groupsWithNames)) {
        group = Optional.empty();
      } else {
        group = Optional.of(groupsWithNames.get(groupsWithNames.size() - 1));
      }
    }

    if (group.isPresent()) {
      ElastiGroupCapacity capacity = group.get().getCapacity();
      return SpotinstElastigroupRunningCountData.builder()
          .elastigroupMin(capacity.getMinimum())
          .elastigroupMax(capacity.getMaximum())
          .elastigroupTarget(capacity.getTarget())
          .elastigroupName(group.get().getName())
          .build();
    } else {
      return SpotinstElastigroupRunningCountData.builder()
          .elastigroupMin(DEFAULT_ELASTIGROUP_MIN_INSTANCES)
          .elastigroupMax(DEFAULT_ELASTIGROUP_MAX_INSTANCES)
          .elastigroupTarget(DEFAULT_ELASTIGROUP_TARGET_INSTANCES)
          .elastigroupName(DEFAULT_ELASTIGROUP_NAME)
          .build();
    }
  }

  private SpotInstConfig validateAndGetSpotinstConfig(String computeProviderSettingId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderSettingId);
    notNullCheck("Compute Provider", computeProviderSetting);
    if (!(computeProviderSetting.getValue() instanceof SpotInstConfig)) {
      throw new InvalidRequestException("Setting Attribute not type of Spotinst config");
    }
    return (SpotInstConfig) computeProviderSetting.getValue();
  }

  @Override
  public List<String> listRoutesForPcf(String appId, String infraDefinitionId) {
    InfrastructureDefinition infrastructureDefinition = get(appId, infraDefinitionId);
    notNullCheck("Infrastructure Definition is null", infrastructureDefinition);

    InfraMappingInfrastructureProvider infrastructure = infrastructureDefinition.getInfrastructure();
    notNullCheck("InfraMappingInfrastructureProvider is null", infrastructure);

    if (!(infrastructure instanceof PcfInfraStructure)) {
      throw new InvalidRequestException("Not PcfInfraStructure, invalid type");
    }

    PcfInfraStructure pcfInfraStructure = (PcfInfraStructure) infrastructure;

    SettingAttribute computeProviderSetting = settingsService.get(pcfInfraStructure.getCloudProviderId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (!(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new InvalidRequestException("Invalid computeProviderSetting");
    }

    if (containsExpression(pcfInfraStructure.getOrganization()) || containsExpression(pcfInfraStructure.getSpace())) {
      return emptyList();
    }

    return pcfHelperService.listRoutes((PcfConfig) computeProviderSetting.getValue(),
        pcfInfraStructure.getOrganization(), pcfInfraStructure.getSpace());
  }

  @Override
  public List<AwsVPC> listVPC(String appId, String computeProviderId, String region) {
    return infrastructureMappingService.listVPC(appId, computeProviderId, region);
  }

  @Override
  public List<AwsSecurityGroup> listSecurityGroups(
      String appId, String computeProviderId, String region, List<String> vpcIds) {
    return infrastructureMappingService.listSecurityGroups(appId, computeProviderId, region, vpcIds);
  }

  @Override
  public List<AwsSubnet> listSubnets(String appId, String computeProviderId, String region, List<String> vpcIds) {
    return infrastructureMappingService.listSubnets(appId, computeProviderId, region, vpcIds);
  }

  @VisibleForTesting
  boolean containsExpression(String value) {
    return isEmpty(value) || value.startsWith("${");
  }
}
