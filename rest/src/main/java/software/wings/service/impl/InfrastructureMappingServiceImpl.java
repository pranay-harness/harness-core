package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.api.DeploymentType.AMI;
import static software.wings.api.DeploymentType.AWS_CODEDEPLOY;
import static software.wings.api.DeploymentType.AWS_LAMBDA;
import static software.wings.api.DeploymentType.ECS;
import static software.wings.api.DeploymentType.HELM;
import static software.wings.api.DeploymentType.KUBERNETES;
import static software.wings.api.DeploymentType.PCF;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.api.DeploymentType.WINRM;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.beans.FeatureName.AZURE_SUPPORT;
import static software.wings.beans.FeatureName.ECS_CREATE_CLUSTER;
import static software.wings.beans.FeatureName.KUBERNETES_CREATE_CLUSTER;
import static software.wings.beans.FeatureName.PIVOTAL_CLOUD_FOUNDRY_SUPPORT;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.exception.WingsException.USER;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.KubernetesConvention.DASH;
import static software.wings.utils.KubernetesConvention.DOT;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Tag;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMappingBase;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.ownership.OwnedByInfrastructureMapping;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.EcsConvention;
import software.wings.utils.HostValidationService;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;
import software.wings.utils.Util;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.validation.Valid;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 1/10/17.
 */
@Singleton
@ValidateOnExecution
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  private static final Logger logger = LoggerFactory.getLogger(InfrastructureMappingServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private Map<String, InfrastructureProvider> infrastructureProviders;
  @Inject private AppService appService;
  @Inject private EnvironmentService envService;
  @Inject private AwsCodeDeployService awsCodeDeployService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private EntityUpdateService entityUpdateService;
  @Inject private ExecutorService executorService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private SettingsService settingsService;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private WorkflowService workflowService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  @Inject private YamlDirectoryService yamlDirectoryService;
  @Inject private SecretManager secretManager;
  @Inject private ExpressionEvaluator evaluator;
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private PcfHelperService pcfHelperService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Override
  public PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest) {
    return list(pageRequest, false);
  }

  @Override
  public PageResponse<InfrastructureMapping> list(
      PageRequest<InfrastructureMapping> pageRequest, boolean disableValidation) {
    PageResponse<InfrastructureMapping> pageResponse =
        wingsPersistence.query(InfrastructureMapping.class, pageRequest, disableValidation);
    if (pageResponse != null && pageResponse.getResponse() != null) {
      for (InfrastructureMapping infrastructureMapping : pageResponse.getResponse()) {
        try {
          setLoadBalancerName(infrastructureMapping);
        } catch (Exception e) {
          logger.error("Failed to set load balancer for InfrastructureMapping {} ", infrastructureMapping, e);
        }
      }
    }
    return pageResponse;
  }

  private void setLoadBalancerName(InfrastructureMapping infrastructureMapping) {
    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      ((AwsInfrastructureMapping) infrastructureMapping)
          .setLoadBalancerName(((AwsInfrastructureMapping) infrastructureMapping).getLoadBalancerId());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMappingBase) {
      SettingAttribute settingAttribute =
          settingsService.get(((PhysicalInfrastructureMappingBase) infrastructureMapping).getLoadBalancerId());
      if (settingAttribute != null) {
        ((PhysicalInfrastructureMappingBase) infrastructureMapping).setLoadBalancerName(settingAttribute.getName());
      }
    }
  }

  @Override
  public InfrastructureMapping save(@Valid InfrastructureMapping infraMapping) {
    // The default name uses a bunch of user inputs, which is why we generate it at the time of save.
    if (infraMapping.isAutoPopulate()) {
      setAutoPopulatedName(infraMapping);
    }

    SettingAttribute computeProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());

    ServiceTemplate serviceTemplate =
        serviceTemplateService.get(infraMapping.getAppId(), infraMapping.getServiceTemplateId());
    notNullCheck("Service Template", serviceTemplate);

    infraMapping.setServiceId(serviceTemplate.getServiceId());
    if (computeProviderSetting != null) {
      infraMapping.setComputeProviderName(computeProviderSetting.getName());
    }

    if (infraMapping instanceof AwsInfrastructureMapping) {
      ((AwsInfrastructureMapping) infraMapping).validate();
    }

    if (infraMapping instanceof EcsInfrastructureMapping) {
      validateEcsInfraMapping((EcsInfrastructureMapping) infraMapping, computeProviderSetting);
    }

    if (infraMapping instanceof GcpKubernetesInfrastructureMapping) {
      GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping =
          (GcpKubernetesInfrastructureMapping) infraMapping;
      if (isBlank(gcpKubernetesInfrastructureMapping.getNamespace())) {
        gcpKubernetesInfrastructureMapping.setNamespace("default");
      }
      validateGcpInfraMapping(gcpKubernetesInfrastructureMapping, computeProviderSetting);
    }

    if (infraMapping instanceof AzureKubernetesInfrastructureMapping) {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          (AzureKubernetesInfrastructureMapping) infraMapping;
      validateAzureInfraMapping(azureKubernetesInfrastructureMapping);
    }

    if (infraMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          (DirectKubernetesInfrastructureMapping) infraMapping;
      if (isBlank(directKubernetesInfrastructureMapping.getNamespace())) {
        directKubernetesInfrastructureMapping.setNamespace("default");
      }
      validateDirectKubernetesInfraMapping(directKubernetesInfrastructureMapping);
    }

    if (infraMapping instanceof PhysicalInfrastructureMapping) {
      validatePyInfraMapping((PhysicalInfrastructureMapping) infraMapping);
    }

    if (infraMapping instanceof PhysicalInfrastructureMappingWinRm) {
      validatePhysicalInfrastructureMappingWinRm((PhysicalInfrastructureMappingWinRm) infraMapping);
    }

    if (infraMapping instanceof PcfInfrastructureMapping) {
      validatePcfInfrastructureMapping((PcfInfrastructureMapping) infraMapping);
    }

    InfrastructureMapping savedInfraMapping = duplicateCheck(
        () -> wingsPersistence.saveAndGet(InfrastructureMapping.class, infraMapping), "name", infraMapping.getName());
    executorService.submit(() -> saveYamlChangeSet(savedInfraMapping, ChangeType.ADD));
    return savedInfraMapping;
  }

  private List<String> getUniqueHostNames(PhysicalInfrastructureMappingBase physicalInfrastructureMapping) {
    List<String> hostNames = physicalInfrastructureMapping.getHostNames()
                                 .stream()
                                 .map(String::trim)
                                 .filter(StringUtils::isNotEmpty)
                                 .distinct()
                                 .collect(toList());
    if (hostNames.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Host names must not be empty");
    }
    return hostNames;
  }

  private void saveYamlChangeSet(InfrastructureMapping infraMapping, ChangeType crudType) {
    String accountId = appService.getAccountIdByAppId(infraMapping.getAppId());
    YamlGitConfig ygs = yamlDirectoryService.weNeedToPushChanges(accountId);
    if (ygs != null) {
      yamlChangeSetService.saveChangeSet(
          ygs, asList(entityUpdateService.getInfraMappingGitSyncFile(accountId, infraMapping, crudType)));
    }
  }

  /**
   * This method gets the default name, checks if another entry exists with the same name, if exists, it parses and
   * extracts the chartVersion and creates a name with the next chartVersion.
   *
   * @param infraMapping
   */
  private void setAutoPopulatedName(InfrastructureMapping infraMapping) {
    String name = infraMapping.getDefaultName();

    String escapedString = Pattern.quote(name);

    // We need to check if the name exists in case of auto generate, if it exists, we need to add a suffix to the name.
    PageRequest<InfrastructureMapping> pageRequest = aPageRequest()
                                                         .addFilter("appId", Operator.EQ, infraMapping.getAppId())
                                                         .addFilter("envId", Operator.EQ, infraMapping.getEnvId())
                                                         .addFilter("name", Operator.STARTS_WITH, escapedString)
                                                         .addOrder("name", OrderType.DESC)
                                                         .build();
    PageResponse<InfrastructureMapping> response = wingsPersistence.query(InfrastructureMapping.class, pageRequest);

    // If an entry exists with the given default name
    if (isNotEmpty(response)) {
      String existingName = response.get(0).getName();
      name = Util.getNameWithNextRevision(existingName, name);
    }

    infraMapping.setName(name);
  }

  private void validateEcsInfraMapping(EcsInfrastructureMapping infraMapping, SettingAttribute computeProviderSetting) {
    if (Constants.RUNTIME.equals(infraMapping.getClusterName())) {
      if (!featureFlagService.isEnabled(ECS_CREATE_CLUSTER, computeProviderSetting.getAccountId())) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Creating a cluster at runtime is not yet supported for ECS.");
      }
    }
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    Validator.notNullCheck("SettingAttribute", settingAttribute);
    String clusterName = infraMapping.getClusterName();
    String region = infraMapping.getRegion();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptionDetails)
                                                        .clusterName(clusterName)
                                                        .region(region)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  private void validateGcpInfraMapping(
      GcpKubernetesInfrastructureMapping infraMapping, SettingAttribute computeProviderSetting) {
    if (Constants.RUNTIME.equals(infraMapping.getClusterName())) {
      if (!featureFlagService.isEnabled(KUBERNETES_CREATE_CLUSTER, computeProviderSetting.getAccountId())) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "Creating a cluster at runtime is not yet supported for Kubernetes.");
      }
    }
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    Validator.notNullCheck("SettingAttribute", settingAttribute);
    String clusterName = infraMapping.getClusterName();
    String namespace = infraMapping.getNamespace();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptionDetails)
                                                        .clusterName(clusterName)
                                                        .namespace(namespace)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  private void validateAzureInfraMapping(AzureKubernetesInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    Validator.notNullCheck("SettingAttribute", settingAttribute);
    String clusterName = infraMapping.getClusterName();
    String subscriptionId = infraMapping.getSubscriptionId();
    String resourceGroup = infraMapping.getResourceGroup();
    String namespace = infraMapping.getNamespace();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptionDetails)
                                                        .clusterName(clusterName)
                                                        .subscriptionId(subscriptionId)
                                                        .resourceGroup(resourceGroup)
                                                        .namespace(namespace)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  private void validateDirectKubernetesInfraMapping(DirectKubernetesInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute =
        (infraMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name()))
        ? aSettingAttribute().withValue(infraMapping.createKubernetesConfig()).build()
        : settingsService.get(infraMapping.getComputeProviderSettingId());
    String namespace = infraMapping.getNamespace();

    List<EncryptedDataDetail> encryptionDetails =
        (infraMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name()))
        ? emptyList()
        : secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptionDetails)
                                                        .namespace(namespace)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      throw new InvalidRequestException(Misc.getMessage(e), USER);
    }
  }

  @Override
  public InfrastructureMapping get(String appId, String infraMappingId) {
    return wingsPersistence.get(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping) {
    InfrastructureMapping savedInfraMapping = get(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());

    Map<String, Object> keyValuePairs = new HashMap<>();
    Set<String> fieldsToRemove = new HashSet<>();
    keyValuePairs.put("computeProviderSettingId", infrastructureMapping.getComputeProviderSettingId());

    if (savedInfraMapping.getHostConnectionAttrs() != null
        && !savedInfraMapping.getHostConnectionAttrs().equals(infrastructureMapping.getHostConnectionAttrs())) {
      getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType())
          .updateHostConnAttrs(infrastructureMapping, infrastructureMapping.getHostConnectionAttrs());
      keyValuePairs.put("hostConnectionAttrs", infrastructureMapping.getHostConnectionAttrs());
    }

    if (isNotEmpty(infrastructureMapping.getName())) {
      keyValuePairs.put("name", infrastructureMapping.getName());
    } else {
      fieldsToRemove.add("name");
    }

    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      validateEcsInfraMapping(ecsInfrastructureMapping, computeProviderSetting);
      handleEcsInfraMapping(keyValuePairs, ecsInfrastructureMapping);

    } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          (DirectKubernetesInfrastructureMapping) infrastructureMapping;
      validateDirectKubernetesInfraMapping(directKubernetesInfrastructureMapping);
      if (directKubernetesInfrastructureMapping.getMasterUrl() != null) {
        keyValuePairs.put("masterUrl", directKubernetesInfrastructureMapping.getMasterUrl());
      } else {
        fieldsToRemove.add("masterUrl");
      }
      if (directKubernetesInfrastructureMapping.getUsername() != null) {
        keyValuePairs.put("username", directKubernetesInfrastructureMapping.getUsername());
      } else {
        fieldsToRemove.add("username");
      }
      if (directKubernetesInfrastructureMapping.getPassword() != null) {
        keyValuePairs.put("password", directKubernetesInfrastructureMapping.getPassword());
      } else {
        fieldsToRemove.add("password");
      }
      if (directKubernetesInfrastructureMapping.getCaCert() != null) {
        keyValuePairs.put("caCert", directKubernetesInfrastructureMapping.getCaCert());
      } else {
        fieldsToRemove.add("caCert");
      }
      if (directKubernetesInfrastructureMapping.getClientCert() != null) {
        keyValuePairs.put("clientCert", directKubernetesInfrastructureMapping.getClientCert());
      } else {
        fieldsToRemove.add("clientCert");
      }
      if (directKubernetesInfrastructureMapping.getClientKey() != null) {
        keyValuePairs.put("clientKey", directKubernetesInfrastructureMapping.getClientKey());
      } else {
        fieldsToRemove.add("clientKey");
      }
      if (directKubernetesInfrastructureMapping.getClientKeyPassphrase() != null) {
        keyValuePairs.put("clientKeyPassphrase", directKubernetesInfrastructureMapping.getClientKeyPassphrase());
      } else {
        fieldsToRemove.add("clientKeyPassphrase");
      }
      if (directKubernetesInfrastructureMapping.getClientKeyAlgo() != null) {
        keyValuePairs.put("clientKeyAlgo", directKubernetesInfrastructureMapping.getClientKeyAlgo());
      } else {
        fieldsToRemove.add("clientKeyAlgo");
      }
      if (isNotBlank(directKubernetesInfrastructureMapping.getNamespace())) {
        keyValuePairs.put("namespace", directKubernetesInfrastructureMapping.getNamespace());
      } else {
        directKubernetesInfrastructureMapping.setNamespace("default");
        keyValuePairs.put("namespace", "default");
      }
      if (directKubernetesInfrastructureMapping.getClusterName() != null) {
        keyValuePairs.put("clusterName", directKubernetesInfrastructureMapping.getClusterName());
      } else {
        fieldsToRemove.add("clusterName");
      }
    } else if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping =
          (GcpKubernetesInfrastructureMapping) infrastructureMapping;
      validateGcpInfraMapping(gcpKubernetesInfrastructureMapping, computeProviderSetting);
      keyValuePairs.put("clusterName", gcpKubernetesInfrastructureMapping.getClusterName());
      keyValuePairs.put("namespace",
          isNotBlank(gcpKubernetesInfrastructureMapping.getNamespace())
              ? gcpKubernetesInfrastructureMapping.getNamespace()
              : "default");
    } else if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          (AzureKubernetesInfrastructureMapping) infrastructureMapping;
      validateAzureInfraMapping(azureKubernetesInfrastructureMapping);
      keyValuePairs.put("clusterName", azureKubernetesInfrastructureMapping.getClusterName());
      keyValuePairs.put("subscriptionId", azureKubernetesInfrastructureMapping.getSubscriptionId());
      keyValuePairs.put("resourceGroup", azureKubernetesInfrastructureMapping.getResourceGroup());
      keyValuePairs.put("namespace",
          isNotBlank(azureKubernetesInfrastructureMapping.getNamespace())
              ? azureKubernetesInfrastructureMapping.getNamespace()
              : "default");
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMapping;
      awsInfrastructureMapping.validate();
      keyValuePairs.put("region", awsInfrastructureMapping.getRegion());
      if (isNotEmpty(awsInfrastructureMapping.getLoadBalancerId())) {
        keyValuePairs.put("loadBalancerId", awsInfrastructureMapping.getLoadBalancerId());
      } else {
        fieldsToRemove.add("loadBalancerId");
      }
      keyValuePairs.put("usePublicDns", awsInfrastructureMapping.isUsePublicDns());
      keyValuePairs.put("setDesiredCapacity", awsInfrastructureMapping.isSetDesiredCapacity());
      keyValuePairs.put("hostNameConvention", awsInfrastructureMapping.getHostNameConvention());
      keyValuePairs.put("desiredCapacity", awsInfrastructureMapping.getDesiredCapacity());
      keyValuePairs.put("provisionInstances", awsInfrastructureMapping.isProvisionInstances());
      if (awsInfrastructureMapping.getAwsInstanceFilter() != null) {
        keyValuePairs.put("awsInstanceFilter", awsInfrastructureMapping.getAwsInstanceFilter());
      } else {
        fieldsToRemove.add("awsInstanceFilter");
      }
      if (isNotEmpty(awsInfrastructureMapping.getAutoScalingGroupName())) {
        keyValuePairs.put("autoScalingGroupName", awsInfrastructureMapping.getAutoScalingGroupName());
      } else {
        fieldsToRemove.add("autoScalingGroupName");
      }
    } else if (infrastructureMapping instanceof AwsLambdaInfraStructureMapping) {
      AwsLambdaInfraStructureMapping lambdaInfraStructureMapping =
          (AwsLambdaInfraStructureMapping) infrastructureMapping;
      validateAwsLambdaInfrastructureMapping(lambdaInfraStructureMapping);
      keyValuePairs.put("region", lambdaInfraStructureMapping.getRegion());
      if (lambdaInfraStructureMapping.getVpcId() != null) {
        keyValuePairs.put("vpcId", lambdaInfraStructureMapping.getVpcId());
        keyValuePairs.put("subnetIds", lambdaInfraStructureMapping.getSubnetIds());
        keyValuePairs.put("securityGroupIds", lambdaInfraStructureMapping.getSecurityGroupIds());
      }
      keyValuePairs.put("role", lambdaInfraStructureMapping.getRole());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMapping) {
      validatePyInfraMapping((PhysicalInfrastructureMapping) infrastructureMapping);
      keyValuePairs.put("loadBalancerId", ((PhysicalInfrastructureMapping) infrastructureMapping).getLoadBalancerId());
      keyValuePairs.put("hostNames", ((PhysicalInfrastructureMapping) infrastructureMapping).getHostNames());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMappingWinRm) {
      validatePhysicalInfrastructureMappingWinRm((PhysicalInfrastructureMappingWinRm) infrastructureMapping);
      keyValuePairs.put(
          "loadBalancerId", ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getLoadBalancerId());
      keyValuePairs.put("hostNames", ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getHostNames());
      keyValuePairs.put("winRmConnectionAttributes",
          ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getWinRmConnectionAttributes());
    } else if (infrastructureMapping instanceof CodeDeployInfrastructureMapping) {
      CodeDeployInfrastructureMapping codeDeployInfrastructureMapping =
          (CodeDeployInfrastructureMapping) infrastructureMapping;
      keyValuePairs.put("region", codeDeployInfrastructureMapping.getRegion());
      keyValuePairs.put("applicationName", codeDeployInfrastructureMapping.getApplicationName());
      keyValuePairs.put("deploymentGroup", codeDeployInfrastructureMapping.getDeploymentGroup());
      keyValuePairs.put("deploymentConfig", codeDeployInfrastructureMapping.getDeploymentConfig());
      keyValuePairs.put("hostNameConvention", codeDeployInfrastructureMapping.getHostNameConvention());
    } else if (infrastructureMapping instanceof AwsAmiInfrastructureMapping) {
      AwsAmiInfrastructureMapping awsAmiInfrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;
      keyValuePairs.put("region", awsAmiInfrastructureMapping.getRegion());
      keyValuePairs.put("autoScalingGroupName", awsAmiInfrastructureMapping.getAutoScalingGroupName());
      if (awsAmiInfrastructureMapping.getClassicLoadBalancers() != null) {
        keyValuePairs.put("classicLoadBalancers", awsAmiInfrastructureMapping.getClassicLoadBalancers());
      }
      if (awsAmiInfrastructureMapping.getTargetGroupArns() != null) {
        keyValuePairs.put("targetGroupArns", awsAmiInfrastructureMapping.getTargetGroupArns());
      }
      keyValuePairs.put("hostNameConvention", awsAmiInfrastructureMapping.getHostNameConvention());
    } else if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      PcfInfrastructureMapping pcfInfrastructureMapping = (PcfInfrastructureMapping) infrastructureMapping;
      validatePcfInfrastructureMapping(pcfInfrastructureMapping);
      handlePcfInfraMapping(keyValuePairs, pcfInfrastructureMapping);
    }
    if (computeProviderSetting != null) {
      keyValuePairs.put("computeProviderName", computeProviderSetting.getName());
    }
    wingsPersistence.updateFields(
        infrastructureMapping.getClass(), infrastructureMapping.getUuid(), keyValuePairs, fieldsToRemove);
    InfrastructureMapping updatedInfraMapping = get(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    yamlChangeSetHelper.updateYamlChangeAsync(updatedInfraMapping, savedInfraMapping, savedInfraMapping.getAccountId());
    return updatedInfraMapping;
  }

  private void handlePcfInfraMapping(
      Map<String, Object> keyValuePairs, PcfInfrastructureMapping pcfInfrastructureMapping) {
    keyValuePairs.put("organization", pcfInfrastructureMapping.getOrganization());
    keyValuePairs.put("space", pcfInfrastructureMapping.getSpace());
    keyValuePairs.put("tempRouteMap",
        pcfInfrastructureMapping.getTempRouteMap() == null ? Collections.EMPTY_LIST
                                                           : pcfInfrastructureMapping.getTempRouteMap());
    keyValuePairs.put("routeMaps",
        pcfInfrastructureMapping.getRouteMaps() == null ? Collections.EMPTY_LIST
                                                        : pcfInfrastructureMapping.getRouteMaps());
  }

  private void handleEcsInfraMapping(
      Map<String, Object> keyValuePairs, EcsInfrastructureMapping ecsInfrastructureMapping) {
    keyValuePairs.put("clusterName", ecsInfrastructureMapping.getClusterName());
    keyValuePairs.put("region", ecsInfrastructureMapping.getRegion());
    keyValuePairs.put("assignPublicIp", ecsInfrastructureMapping.isAssignPublicIp());
    keyValuePairs.put("launchType", ecsInfrastructureMapping.getLaunchType());
    keyValuePairs.put(
        "vpcId", ecsInfrastructureMapping.getVpcId() == null ? StringUtils.EMPTY : ecsInfrastructureMapping.getVpcId());
    keyValuePairs.put("subnetIds",
        ecsInfrastructureMapping.getSubnetIds() == null ? Collections.EMPTY_LIST
                                                        : ecsInfrastructureMapping.getSubnetIds());
    keyValuePairs.put("securityGroupIds",
        ecsInfrastructureMapping.getSecurityGroupIds() == null ? Collections.EMPTY_LIST
                                                               : ecsInfrastructureMapping.getSecurityGroupIds());
    keyValuePairs.put("executionRole",
        ecsInfrastructureMapping.getExecutionRole() == null ? StringUtils.EMPTY
                                                            : ecsInfrastructureMapping.getExecutionRole());
  }

  private void validatePyInfraMapping(PhysicalInfrastructureMapping pyInfraMapping) {
    pyInfraMapping.setHostNames(getUniqueHostNames(pyInfraMapping));
  }

  private void validatePhysicalInfrastructureMappingWinRm(PhysicalInfrastructureMappingWinRm infraMapping) {
    infraMapping.setHostNames(getUniqueHostNames(infraMapping));

    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    Validator.notNullCheck("ComputeProviderSettingAttribute", settingAttribute);

    settingAttribute = settingsService.get(infraMapping.getWinRmConnectionAttributes());
    Validator.notNullCheck("WinRmConnectionAttributes", settingAttribute);
  }

  private void validatePcfInfrastructureMapping(PcfInfrastructureMapping infraMapping) {
    if (StringUtils.isBlank(infraMapping.getOrganization()) || StringUtils.isBlank(infraMapping.getSpace())) {
      logger.error("For PCFInfraMapping, Org and Space value cant be null");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "Host names must be unique");
    }

    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    Validator.notNullCheck("ComputeProviderSettingAttribute", settingAttribute);
  }

  private void validateAwsLambdaInfrastructureMapping(AwsLambdaInfraStructureMapping lambdaInfraStructureMapping) {
    if (lambdaInfraStructureMapping.getVpcId() != null) {
      if (lambdaInfraStructureMapping.getSubnetIds().isEmpty()) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "At least one subnet must be provided");
      }
      if (lambdaInfraStructureMapping.getSecurityGroupIds().isEmpty()) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT)
            .addParam("args", "At least one security group must be provided");
      }
    }
  }

  @Override
  public void delete(String appId, String infraMappingId) {
    delete(appId, infraMappingId, false);
  }

  private void delete(String appId, String infraMappingId, boolean forceDelete) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    if (infrastructureMapping == null) {
      return;
    }

    if (!forceDelete) {
      ensureInfraMappingSafeToDelete(infrastructureMapping);
    }

    saveYamlChangeSet(infrastructureMapping, ChangeType.DELETE);

    prune(appId, infraMappingId);
  }

  private void prune(String appId, String infraMappingId) {
    PruneEntityJob.addDefaultJob(
        jobScheduler, InfrastructureMapping.class, appId, infraMappingId, Duration.ofSeconds(5));

    wingsPersistence.delete(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    List<Key<InfrastructureMapping>> keys = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                .filter(InfrastructureMapping.APP_ID_KEY, appId)
                                                .filter("envId", envId)
                                                .asKeyList();
    for (Key<InfrastructureMapping> key : keys) {
      prune(appId, (String) key.getId());
    }
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraMappingId) {
    List<OwnedByInfrastructureMapping> services = ServiceClassLocator.descendingServices(
        this, InfrastructureMappingServiceImpl.class, OwnedByInfrastructureMapping.class);
    PruneEntityJob.pruneDescendingEntities(
        services, descending -> descending.pruneByInfrastructureMapping(appId, infraMappingId));
  }

  private void ensureInfraMappingSafeToDelete(InfrastructureMapping infrastructureMapping) {
    List<Workflow> workflows = workflowService
                                   .listWorkflows(aPageRequest()
                                                      .withLimit(UNLIMITED)
                                                      .addFilter("appId", Operator.EQ, infrastructureMapping.getAppId())
                                                      .build())
                                   .getResponse();

    List<String> referencingWorkflowNames =
        workflows.stream()
            .filter(wfl -> {
              if (wfl.getOrchestrationWorkflow() != null
                  && wfl.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
                Map<String, WorkflowPhase> workflowPhaseIdMap =
                    ((CanaryOrchestrationWorkflow) wfl.getOrchestrationWorkflow()).getWorkflowPhaseIdMap();
                return workflowPhaseIdMap.values().stream().anyMatch(workflowPhase
                    -> !workflowPhase.checkInfraTemplatized()
                        && infrastructureMapping.getUuid().equals(workflowPhase.getInfraMappingId()));
              }
              return false;
            })
            .map(Workflow::getName)
            .collect(toList());

    if (!referencingWorkflowNames.isEmpty()) {
      throw new WingsException(INVALID_REQUEST, USER)
          .addParam("message",
              String.format("Service Infrastructure is in use by %d %s [%s].", referencingWorkflowNames.size(),
                  plural("workflow", referencingWorkflowNames.size()), Joiner.on(", ").join(referencingWorkflowNames)));
    }
  }

  @Override
  public void deleteByServiceTemplate(String appId, String serviceTemplateId) {
    List<Key<InfrastructureMapping>> keys = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                .filter("appId", appId)
                                                .filter("serviceTemplateId", serviceTemplateId)
                                                .asKeyList();
    keys.forEach(key -> delete(appId, (String) key.getId(), true));
  }

  @Override
  public Map<String, Object> getInfraMappingStencils(String appId) {
    return stencilPostProcessor.postProcess(Lists.newArrayList(InfrastructureMappingType.values()), appId)
        .stream()
        .collect(toMap(Stencil::getName, Function.identity()));
  }

  @Override
  public List<ServiceInstance> selectServiceInstances(
      String appId, String infraMappingId, String workflowExecutionId, ServiceInstanceSelectionParams selectionParams) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Infra Mapping", infrastructureMapping);

    List<Host> hosts;
    if (infrastructureMapping instanceof AwsInfrastructureMapping
        && ((AwsInfrastructureMapping) infrastructureMapping).isProvisionInstances()) {
      hosts = getAutoScaleGroupNodes(appId, infraMappingId, workflowExecutionId);
    } else {
      hosts = listHosts(infrastructureMapping)
                  .stream()
                  .filter(host
                      -> !selectionParams.isSelectSpecificHosts()
                          || selectionParams.getHostNames().contains(host.getPublicDns()))
                  .collect(toList());
    }

    int count =
        selectionParams.isSelectSpecificHosts() ? selectionParams.getHostNames().size() : selectionParams.getCount();
    List<String> excludedServiceInstanceIds = selectionParams.getExcludedServiceInstanceIds();
    return syncHostsAndUpdateInstances(infrastructureMapping, hosts)
        .stream()
        .filter(serviceInstance -> !excludedServiceInstanceIds.contains(serviceInstance.getUuid()))
        .limit(count)
        .collect(toList());
  }

  @Override
  public List<Host> listHosts(String appId, String infrastructureMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infrastructureMappingId);
    notNullCheck("Infra Mapping", infrastructureMapping);
    return listHosts(infrastructureMapping);
  }

  private List<Host> listHosts(InfrastructureMapping infrastructureMapping) {
    if (infrastructureMapping instanceof PhysicalInfrastructureMapping) {
      PhysicalInfrastructureMapping pyInfraMapping = (PhysicalInfrastructureMapping) infrastructureMapping;
      List<String> hostNames = pyInfraMapping.getHostNames()
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
                     .withAppId(pyInfraMapping.getAppId())
                     .withEnvId(pyInfraMapping.getEnvId())
                     .withInfraMappingId(pyInfraMapping.getUuid())
                     .withHostConnAttr(pyInfraMapping.getHostConnectionAttrs())
                     .withServiceTemplateId(pyInfraMapping.getServiceTemplateId())
                     .build())
          .collect(toList());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMappingWinRm) {
      PhysicalInfrastructureMappingWinRm pyInfraMappingWinRm =
          (PhysicalInfrastructureMappingWinRm) infrastructureMapping;
      List<String> hostNames = pyInfraMappingWinRm.getHostNames()
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
                     .withAppId(pyInfraMappingWinRm.getAppId())
                     .withEnvId(pyInfraMappingWinRm.getEnvId())
                     .withInfraMappingId(pyInfraMappingWinRm.getUuid())
                     .withWinrmConnAttr(pyInfraMappingWinRm.getWinRmConnectionAttributes())
                     .withServiceTemplateId(pyInfraMappingWinRm.getServiceTemplateId())
                     .build())
          .collect(toList());
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureMapping awsInfraMapping = (AwsInfrastructureMapping) infrastructureMapping;
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      SettingAttribute computeProviderSetting = settingsService.get(awsInfraMapping.getComputeProviderSettingId());
      notNullCheck("Compute Provider", computeProviderSetting);
      return infrastructureProvider
          .listHosts(awsInfraMapping, computeProviderSetting,
              secretManager.getEncryptionDetails((Encryptable) computeProviderSetting.getValue(), null, null),
              new PageRequest<>())
          .getResponse();
    } else {
      throw new InvalidRequestException(
          "Unsupported infrastructure mapping: " + infrastructureMapping.getClass().getName());
    }
  }

  private List<ServiceInstance> syncHostsAndUpdateInstances(
      InfrastructureMapping infrastructureMapping, List<Host> hosts) {
    InfrastructureProvider infrastructureProvider =
        getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType());
    ServiceTemplate serviceTemplate =
        serviceTemplateService.get(infrastructureMapping.getAppId(), infrastructureMapping.getServiceTemplateId());

    List<Host> savedHosts = hosts.stream().map(infrastructureProvider::saveHost).collect(toList());

    return serviceInstanceService.updateInstanceMappings(serviceTemplate, infrastructureMapping, savedHosts);
  }

  @Override
  public List<String> listClusters(String appId, String deploymentType, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    String type = computeProviderSetting.getValue().getType();
    if (AWS.name().equals(type)) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listClusterNames(computeProviderSetting, region);
    } else if (GCP.name().equals(type)) {
      GcpInfrastructureProvider infrastructureProvider =
          (GcpInfrastructureProvider) getInfrastructureProviderByComputeProviderType(GCP.name());
      return infrastructureProvider.listClusterNames(computeProviderSetting,
          secretManager.getEncryptionDetails((Encryptable) computeProviderSetting.getValue(), null, null));
    }
    return emptyList();
  }

  @Override
  public List<String> listRegions(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listRegions(computeProviderSetting);
    }
    return emptyList();
  }

  @Override
  public List<String> listInstanceTypes(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listInstanceTypes(computeProviderSetting);
    }
    return emptyList();
  }

  @Override
  public List<String> listInstanceRoles(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listIAMInstanceRoles(computeProviderSetting);
    }
    return emptyList();
  }

  @Override
  public Map<String, String> listAwsIamRoles(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);
    return listAllRoles(appId, infrastructureMapping.getComputeProviderSettingId());
  }

  @Override
  public Set<String> listTags(String appId, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listTags(computeProviderSetting, region);
    }
    return Collections.emptySet();
  }

  @Override
  public List<String> listAutoScalingGroups(String appId, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listAutoScalingGroups(computeProviderSetting, region);
    }
    return emptyList();
  }

  @Override
  public Map<String, String> listAlbTargetGroups(String appId, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listTargetGroups(computeProviderSetting, region, null);
    }
    return emptyMap();
  }

  @Override
  public Map<String, String> listAllRoles(String appId, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listIAMRoles(computeProviderSetting);
    }
    return Collections.emptyMap();
  }

  @Override
  public List<String> listVPC(String appId, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listVPCs(computeProviderSetting, region);
    }
    return emptyList();
  }

  public List<String> listOrganizationsForPcf(String appId, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }
    return pcfHelperService.listOrganizations((PcfConfig) computeProviderSetting.getValue());
  }

  @Override
  public List<String> listSpacesForPcf(String appId, String computeProviderId, String organization) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }
    return pcfHelperService.listSpaces((PcfConfig) computeProviderSetting.getValue(), organization);
  }

  @Override
  public List<String> lisRouteMapsForPcf(String appId, String computeProviderId, String organization, String spaces) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }

    return pcfHelperService.listRoutes((PcfConfig) computeProviderSetting.getValue(), organization, spaces);
  }

  @Override
  public List<String> listSecurityGroups(String appId, String computeProviderId, String region, List<String> vpcIds) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listSecurityGroups(computeProviderSetting, region, vpcIds);
    }
    return emptyList();
  }

  @Override
  public List<String> listSubnets(String appId, String computeProviderId, String region, List<String> vpcIds) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listSubnets(computeProviderSetting, region, vpcIds);
    }
    return emptyList();
  }

  @Override
  public Map<String, String> listLoadBalancers(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listLoadBalancers(computeProviderSetting, Regions.US_EAST_1.getName())
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
  public Map<String, String> listLoadBalancers(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (infrastructureMapping instanceof AwsInfrastructureMapping
        || infrastructureMapping instanceof EcsInfrastructureMapping) {
      String region = infrastructureMapping instanceof AwsInfrastructureMapping
          ? ((AwsInfrastructureMapping) infrastructureMapping).getRegion()
          : ((EcsInfrastructureMapping) infrastructureMapping).getRegion();

      return ((AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name()))
          .listLoadBalancers(computeProviderSetting, region)
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
  public List<String> listClassicLoadBalancers(String appId, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listClassicLoadBalancers(computeProviderSetting, region);
    }
    return emptyList();
  }

  @Override
  public Map<String, String> listTargetGroups(
      String appId, String deploymentType, String computeProviderId, String loadBalancerName) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listTargetGroups(
          computeProviderSetting, Regions.US_EAST_1.getName(), loadBalancerName);
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, String> listTargetGroups(String appId, String infraMappingId, String loadbalancerName) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (infrastructureMapping instanceof AwsInfrastructureMapping
        || infrastructureMapping instanceof EcsInfrastructureMapping) {
      String region = infrastructureMapping instanceof AwsInfrastructureMapping
          ? ((AwsInfrastructureMapping) infrastructureMapping).getRegion()
          : ((EcsInfrastructureMapping) infrastructureMapping).getRegion();
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      return infrastructureProvider.listTargetGroups(computeProviderSetting, region, loadbalancerName);
    }
    return Collections.emptyMap();
  }

  @Override
  public List<HostValidationResponse> validateHost(@Valid HostValidationRequest validationRequest) {
    SettingAttribute computeProviderSetting = settingsService.get(validationRequest.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (!PHYSICAL_DATA_CENTER.name().equals(computeProviderSetting.getValue().getType())) {
      throw new InvalidRequestException("Invalid infrastructure provider");
    }

    SettingAttribute hostConnectionSetting = settingsService.get(validationRequest.getHostConnectionAttrs());
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) hostConnectionSetting.getValue(), null, null);
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(hostConnectionSetting.getAccountId()).withAppId(validationRequest.getAppId()).build();
    return delegateProxyFactory.get(HostValidationService.class, syncTaskContext)
        .validateHost(validationRequest.getHostNames(), hostConnectionSetting, encryptionDetails,
            validationRequest.getExecutionCredential());
  }

  @Override
  public List<String> listElasticLoadBalancer(String accessKey, char[] secretKey, String region) {
    AwsInfrastructureProvider infrastructureProvider =
        (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
    return infrastructureProvider.listClassicLoadBalancers(accessKey, secretKey, region);
  }

  @Override
  public List<String> listCodeDeployApplicationNames(String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      return awsCodeDeployService.listApplications(region, computeProviderSetting,
          secretManager.getEncryptionDetails((Encryptable) computeProviderSetting.getValue(), null, null));
    }
    return ImmutableList.of();
  }

  @Override
  public List<String> listCodeDeployDeploymentGroups(String computeProviderId, String region, String applicationName) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      return awsCodeDeployService.listDeploymentGroup(region, applicationName, computeProviderSetting,
          secretManager.getEncryptionDetails((Encryptable) computeProviderSetting.getValue(), null, null));
    }
    return ImmutableList.of();
  }

  @Override
  public List<String> listCodeDeployDeploymentConfigs(String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      return awsCodeDeployService.listDeploymentConfiguration(region, computeProviderSetting,
          secretManager.getEncryptionDetails((Encryptable) computeProviderSetting.getValue(), null, null));
    }
    return ImmutableList.of();
  }

  @Override
  public List<String> listHostDisplayNames(String appId, String infraMappingId, String workflowExecutionId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Infra Mapping", infrastructureMapping);
    return getInfrastructureMappingHostDisplayNames(infrastructureMapping, appId, workflowExecutionId);
  }

  @Override
  public List<String> listComputeProviderHostDisplayNames(
      String appId, String envId, String serviceId, String computeProviderId) {
    Object serviceTemplateId =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0).getId();
    InfrastructureMapping infrastructureMapping = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                      .filter("appId", appId)
                                                      .filter("envId", envId)
                                                      .filter("serviceTemplateId", serviceTemplateId)
                                                      .filter("computeProviderSettingId", computeProviderId)
                                                      .get();
    notNullCheck("Infra Mapping", infrastructureMapping);

    return getInfrastructureMappingHostDisplayNames(infrastructureMapping, appId, null);
  }

  private List<String> getInfrastructureMappingHostDisplayNames(
      InfrastructureMapping infrastructureMapping, String appId, String workflowExecutionId) {
    if (infrastructureMapping instanceof PhysicalInfrastructureMappingBase) {
      return ((PhysicalInfrastructureMappingBase) infrastructureMapping).getHostNames();
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMapping;
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      SettingAttribute computeProviderSetting =
          settingsService.get(awsInfrastructureMapping.getComputeProviderSettingId());
      notNullCheck("Compute Provider", computeProviderSetting);
      List<Host> hosts = infrastructureProvider
                             .listHosts(awsInfrastructureMapping, computeProviderSetting,
                                 secretManager.getEncryptionDetails(
                                     (Encryptable) computeProviderSetting.getValue(), appId, workflowExecutionId),
                                 new PageRequest<>())
                             .getResponse();
      List<String> hostDisplayNames = new ArrayList<>();
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
    }
    return emptyList();
  }

  @Override
  public String getContainerRunningInstances(String appId, String infraMappingId, String serviceNameExpression) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);

    if (infrastructureMapping == null) {
      return "0";
    }

    Application app = appService.get(infrastructureMapping.getAppId());
    Environment env = envService.get(infrastructureMapping.getAppId(), infrastructureMapping.getEnvId(), false);
    Service service =
        serviceResourceService.get(infrastructureMapping.getAppId(), infrastructureMapping.getServiceId());

    Map<String, Object> context = new HashMap<>();
    context.put("app", app);
    context.put("env", env);
    context.put("service", service);

    SettingAttribute settingAttribute;
    String clusterName = null;
    String namespace = null;
    String containerServiceName = null;
    String region = null;
    String subscriptionId = null;
    String resourceGroup = null;
    ContainerInfrastructureMapping containerInfraMapping = (ContainerInfrastructureMapping) infrastructureMapping;
    boolean isStatefulSet = false;
    ContainerTask containerTask = serviceResourceService.getContainerTaskByDeploymentType(
        app.getUuid(), service.getUuid(), infrastructureMapping.getDeploymentType());
    if (containerTask instanceof KubernetesContainerTask) {
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      isStatefulSet = kubernetesContainerTask.checkStatefulSet();
    }
    if (containerInfraMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directInfraMapping =
          (DirectKubernetesInfrastructureMapping) containerInfraMapping;
      settingAttribute = (directInfraMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name()))
          ? aSettingAttribute().withValue(directInfraMapping.createKubernetesConfig()).build()
          : settingsService.get(directInfraMapping.getComputeProviderSettingId());
      namespace = directInfraMapping.getNamespace();
      containerServiceName =
          (isNotBlank(serviceNameExpression)
                  ? KubernetesConvention.normalize(evaluator.substitute(serviceNameExpression, context))
                  : KubernetesConvention.getControllerNamePrefix(
                        app.getName(), service.getName(), env.getName(), isStatefulSet))
          + (isStatefulSet ? DASH : DOT) + "0";
    } else {
      settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      clusterName = containerInfraMapping.getClusterName();
      if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
        namespace = containerInfraMapping.getNamespace();
        containerServiceName =
            (isNotBlank(serviceNameExpression)
                    ? KubernetesConvention.normalize(evaluator.substitute(serviceNameExpression, context))
                    : KubernetesConvention.getControllerNamePrefix(
                          app.getName(), service.getName(), env.getName(), isStatefulSet))
            + (isStatefulSet ? DASH : DOT) + "0";
      } else if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
        namespace = containerInfraMapping.getNamespace();
        subscriptionId = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId();
        resourceGroup = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup();
        containerServiceName =
            (isNotBlank(serviceNameExpression)
                    ? KubernetesConvention.normalize(evaluator.substitute(serviceNameExpression, context))
                    : KubernetesConvention.getControllerNamePrefix(
                          app.getName(), service.getName(), env.getName(), isStatefulSet))
            + (isStatefulSet ? DASH : DOT) + "0";

      } else if (containerInfraMapping instanceof EcsInfrastructureMapping) {
        region = ((EcsInfrastructureMapping) containerInfraMapping).getRegion();
        containerServiceName = (isNotBlank(serviceNameExpression)
                                       ? Misc.normalizeExpression(evaluator.substitute(serviceNameExpression, context))
                                       : EcsConvention.getTaskFamily(app.getName(), service.getName(), env.getName()))
            + EcsConvention.DELIMITER + "0";
      }
    }
    Validator.notNullCheck("SettingAttribute", settingAttribute);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    SyncTaskContext syncTaskContext = aContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).build();
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .containerServiceName(containerServiceName)
                                                        .encryptionDetails(encryptionDetails)
                                                        .clusterName(clusterName)
                                                        .namespace(namespace)
                                                        .subscriptionId(subscriptionId)
                                                        .resourceGroup(resourceGroup)
                                                        .region(region)
                                                        .build();
    try {
      Map<String, Integer> activeServiceCounts = delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                                                     .getActiveServiceCounts(containerServiceParams);
      return Integer.toString(activeServiceCounts.values().stream().mapToInt(Integer::intValue).sum());
    } catch (Exception e) {
      logger.warn(Misc.getMessage(e), e);
      return "0";
    }
  }

  private InfrastructureProvider getInfrastructureProviderByComputeProviderType(String computeProviderType) {
    return infrastructureProviders.get(computeProviderType);
  }

  @Override
  public InfrastructureMapping getInfraMappingByName(String appId, String envId, String name) {
    return wingsPersistence.createQuery(InfrastructureMapping.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter("name", name)
        .get();
  }

  @Override
  public List<Host> getAutoScaleGroupNodes(String appId, String infraMappingId, String workflowExecutionId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Infra Mapping", infrastructureMapping);

    if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      SettingAttribute computeProviderSetting =
          settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      notNullCheck("Compute Provider", computeProviderSetting);

      AwsInfrastructureProvider awsInfrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(
              infrastructureMapping.getComputeProviderType());
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMapping;

      return awsInfrastructureProvider.maybeSetAutoScaleCapacityAndGetHosts(
          appId, workflowExecutionId, awsInfrastructureMapping, computeProviderSetting);
    } else {
      throw new InvalidRequestException("Auto Scale groups are only supported for AWS infrastructure mapping");
    }
  }

  @Override
  public Map<DeploymentType, List<SettingVariableTypes>> listInfraTypes(String appId, String envId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    ArtifactType artifactType = service.getArtifactType();
    Map<DeploymentType, List<SettingVariableTypes>> infraTypes = new HashMap<>();

    if (artifactType == ArtifactType.DOCKER) {
      infraTypes.put(ECS, asList(SettingVariableTypes.AWS));
      String accountId = appService.getAccountIdByAppId(appId);
      if (featureFlagService.isEnabled(AZURE_SUPPORT, accountId)) {
        infraTypes.put(KUBERNETES,
            asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.DIRECT,
                SettingVariableTypes.KUBERNETES_CLUSTER));
        if (featureFlagService.isEnabled(FeatureName.HELM, accountId)) {
          infraTypes.put(HELM,
              asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.DIRECT,
                  SettingVariableTypes.KUBERNETES_CLUSTER));
        }
      } else {
        infraTypes.put(KUBERNETES,
            asList(SettingVariableTypes.GCP, SettingVariableTypes.DIRECT, SettingVariableTypes.KUBERNETES_CLUSTER));
        if (featureFlagService.isEnabled(FeatureName.HELM, accountId)) {
          infraTypes.put(HELM,
              asList(SettingVariableTypes.GCP, SettingVariableTypes.DIRECT, SettingVariableTypes.KUBERNETES_CLUSTER));
        }
      }
    } else if (artifactType == ArtifactType.AWS_CODEDEPLOY) {
      infraTypes.put(AWS_CODEDEPLOY, asList(SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.AWS_LAMBDA) {
      infraTypes.put(AWS_LAMBDA, asList(SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.AMI) {
      infraTypes.put(AMI, asList(SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.IIS) {
      infraTypes.put(WINRM, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER));
    } else if (artifactType == ArtifactType.PCF) {
      String accountId = appService.getAccountIdByAppId(appId);
      if (featureFlagService.isEnabled(PIVOTAL_CLOUD_FOUNDRY_SUPPORT, accountId)) {
        infraTypes.put(PCF, asList(SettingVariableTypes.PCF));
      }
    } else {
      infraTypes.put(SSH, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS));
    }

    return infraTypes;
  }

  @Override
  public List<InfrastructureMapping> getInfraStructureMappingsByUuids(String appId, List<String> infraMappingIds) {
    if (isNotEmpty(infraMappingIds)) {
      return wingsPersistence.createQuery(InfrastructureMapping.class)
          .filter("appId", appId)
          .field("uuid")
          .in(infraMappingIds)
          .asList();
    }
    return new ArrayList<>();
  }
}
