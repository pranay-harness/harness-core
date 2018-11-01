package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.allChecks;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
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
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.KubernetesConvention.DASH;
import static software.wings.utils.Validator.duplicateCheck;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Tag;
import com.microsoft.azure.management.compute.VirtualMachine;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.validator.EntityNameValidator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.scheduler.PersistentScheduler;
import io.harness.validation.Create;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
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
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.scheduler.PruneEntityJob;
import software.wings.security.encryption.EncryptedDataDetail;
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
import software.wings.service.intfc.aws.manager.AwsCodeDeployHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEcsHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsIamHelperServiceManager;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.ownership.OwnedByInfrastructureMapping;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.Stencil;
import software.wings.stencils.StencilPostProcessor;
import software.wings.utils.ArtifactType;
import software.wings.utils.EcsConvention;
import software.wings.utils.HostValidationService;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;
import software.wings.utils.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
  // DO NOT DELETE THIS, PRUNE logic needs it
  @SuppressWarnings("unused") @Inject private InstanceService instanceService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private SettingsService settingsService;
  @Inject private StencilPostProcessor stencilPostProcessor;
  @Inject private WorkflowService workflowService;
  @Inject private SecretManager secretManager;
  @Inject private ManagerExpressionEvaluator evaluator;
  @Inject private PcfHelperService pcfHelperService;
  @Inject private AwsEcsHelperServiceManager awsEcsHelperServiceManager;
  @Inject private AwsIamHelperServiceManager awsIamHelperServiceManager;
  @Inject private AwsEc2HelperServiceManager awsEc2HelperServiceManager;
  @Inject private AwsCodeDeployHelperServiceManager awsCodeDeployHelperServiceManager;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private YamlPushService yamlPushService;
  @Inject private AzureHelperService azureHelperService;

  @Override
  public PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest) {
    return list(pageRequest, allChecks);
  }

  @Override
  public PageResponse<InfrastructureMapping> list(
      PageRequest<InfrastructureMapping> pageRequest, Set<QueryChecks> queryChecks) {
    PageResponse<InfrastructureMapping> pageResponse =
        wingsPersistence.query(InfrastructureMapping.class, pageRequest, queryChecks);
    if (pageResponse != null && pageResponse.getResponse() != null) {
      for (InfrastructureMapping infrastructureMapping : pageResponse.getResponse()) {
        try {
          setLoadBalancerName(infrastructureMapping);
        } catch (Exception e) {
          logger.error(
              format("Failed to set load balancer for InfrastructureMapping %s", infrastructureMapping.toString()), e);
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

  private void validateInfraMapping(@Valid InfrastructureMapping infraMapping, boolean fromYaml) {
    if (fromYaml) {
      logger.info("Ignore validation for InfraMapping created from yaml");
      return;
    }

    if (infraMapping instanceof AwsInfrastructureMapping) {
      ((AwsInfrastructureMapping) infraMapping).validate();
    }

    if (infraMapping instanceof EcsInfrastructureMapping) {
      validateEcsInfraMapping((EcsInfrastructureMapping) infraMapping);
    }

    if (infraMapping instanceof GcpKubernetesInfrastructureMapping) {
      GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping =
          (GcpKubernetesInfrastructureMapping) infraMapping;
      if (isBlank(gcpKubernetesInfrastructureMapping.getNamespace())) {
        gcpKubernetesInfrastructureMapping.setNamespace("default");
      }
      validateGcpInfraMapping(gcpKubernetesInfrastructureMapping);
    }

    if (infraMapping instanceof AzureKubernetesInfrastructureMapping) {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          (AzureKubernetesInfrastructureMapping) infraMapping;
      validateAzureKubernetesInfraMapping(azureKubernetesInfrastructureMapping);
    }

    if (infraMapping instanceof AzureInfrastructureMapping) {
      AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infraMapping;
      validateAzureInfraMapping(azureInfrastructureMapping);
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
  }

  @Override
  @ValidationGroups(Create.class)
  public InfrastructureMapping save(@Valid InfrastructureMapping infraMapping) {
    return save(infraMapping, false);
  }

  @Override
  @ValidationGroups(Create.class)
  public InfrastructureMapping save(InfrastructureMapping infraMapping, boolean fromYaml) {
    // The default name uses a bunch of user inputs, which is why we generate it at the time of save.
    if (infraMapping.isAutoPopulate()) {
      setAutoPopulatedName(infraMapping);
    }

    SettingAttribute computeProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());

    ServiceTemplate serviceTemplate =
        serviceTemplateService.get(infraMapping.getAppId(), infraMapping.getServiceTemplateId());
    notNullCheck("Service Template", serviceTemplate, USER);

    infraMapping.setServiceId(serviceTemplate.getServiceId());
    if (computeProviderSetting != null) {
      infraMapping.setComputeProviderName(computeProviderSetting.getName());
    }

    validateInfraMapping(infraMapping, fromYaml);

    InfrastructureMapping savedInfraMapping = duplicateCheck(
        () -> wingsPersistence.saveAndGet(InfrastructureMapping.class, infraMapping), "name", infraMapping.getName());

    String accountId = appService.getAccountIdByAppId(infraMapping.getAppId());
    yamlPushService.pushYamlChangeSet(
        accountId, null, savedInfraMapping, Type.CREATE, infraMapping.isSyncFromGit(), false);

    return savedInfraMapping;
  }

  @Override
  public InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping, boolean fromYaml) {
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

    if (isNotEmpty(infrastructureMapping.getProvisionerId())) {
      keyValuePairs.put(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureMapping.getProvisionerId());
    } else {
      fieldsToRemove.add(InfrastructureMapping.PROVISIONER_ID_KEY);
    }

    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(ecsInfrastructureMapping, fromYaml);
      handleEcsInfraMapping(keyValuePairs, ecsInfrastructureMapping);

    } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          (DirectKubernetesInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(directKubernetesInfrastructureMapping, fromYaml);
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
      if (directKubernetesInfrastructureMapping.getServiceAccountToken() != null) {
        keyValuePairs.put("serviceAccountToken", directKubernetesInfrastructureMapping.getServiceAccountToken());
      } else {
        fieldsToRemove.add("serviceAccountToken");
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
      validateInfraMapping(gcpKubernetesInfrastructureMapping, fromYaml);
      keyValuePairs.put("clusterName", gcpKubernetesInfrastructureMapping.getClusterName());
      keyValuePairs.put("namespace",
          isNotBlank(gcpKubernetesInfrastructureMapping.getNamespace())
              ? gcpKubernetesInfrastructureMapping.getNamespace()
              : "default");
    } else if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          (AzureKubernetesInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(azureKubernetesInfrastructureMapping, fromYaml);
      keyValuePairs.put("clusterName", azureKubernetesInfrastructureMapping.getClusterName());
      keyValuePairs.put("subscriptionId", azureKubernetesInfrastructureMapping.getSubscriptionId());
      keyValuePairs.put("resourceGroup", azureKubernetesInfrastructureMapping.getResourceGroup());
      keyValuePairs.put("namespace",
          isNotBlank(azureKubernetesInfrastructureMapping.getNamespace())
              ? azureKubernetesInfrastructureMapping.getNamespace()
              : "default");
    } else if (infrastructureMapping instanceof AzureInfrastructureMapping) {
      AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infrastructureMapping;
      validateAzureInfraMapping(azureInfrastructureMapping);
      keyValuePairs.put("subscriptionId", azureInfrastructureMapping.getSubscriptionId());
      keyValuePairs.put("resourceGroup", azureInfrastructureMapping.getResourceGroup());
      keyValuePairs.put("winRmConnectionAttributes", azureInfrastructureMapping.getWinRmConnectionAttributes());
      keyValuePairs.put("tags", azureInfrastructureMapping.getTags());
      keyValuePairs.put("usePublicDns", azureInfrastructureMapping.isUsePublicDns());
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(awsInfrastructureMapping, fromYaml);
      if (awsInfrastructureMapping.getRegion() != null) {
        keyValuePairs.put("region", awsInfrastructureMapping.getRegion());
      } else {
        fieldsToRemove.add("region");
      }
      if (isNotEmpty(awsInfrastructureMapping.getLoadBalancerId())) {
        keyValuePairs.put("loadBalancerId", awsInfrastructureMapping.getLoadBalancerId());
      } else {
        fieldsToRemove.add("loadBalancerId");
      }
      keyValuePairs.put("usePublicDns", awsInfrastructureMapping.isUsePublicDns());
      keyValuePairs.put("setDesiredCapacity", awsInfrastructureMapping.isSetDesiredCapacity());
      if (awsInfrastructureMapping.getHostNameConvention() != null) {
        keyValuePairs.put("hostNameConvention", awsInfrastructureMapping.getHostNameConvention());
      } else {
        fieldsToRemove.add("hostNameConvention");
      }
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
      validateInfraMapping(lambdaInfraStructureMapping, fromYaml);
      if (lambdaInfraStructureMapping.getRegion() != null) {
        keyValuePairs.put("region", lambdaInfraStructureMapping.getRegion());
      } else {
        fieldsToRemove.add("region");
      }
      if (lambdaInfraStructureMapping.getVpcId() != null) {
        keyValuePairs.put("vpcId", lambdaInfraStructureMapping.getVpcId());
        keyValuePairs.put("subnetIds", lambdaInfraStructureMapping.getSubnetIds());
        keyValuePairs.put("securityGroupIds", lambdaInfraStructureMapping.getSecurityGroupIds());
      }
      keyValuePairs.put("role", lambdaInfraStructureMapping.getRole());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMapping) {
      validateInfraMapping(infrastructureMapping, fromYaml);
      keyValuePairs.put("loadBalancerId", ((PhysicalInfrastructureMapping) infrastructureMapping).getLoadBalancerId());
      keyValuePairs.put("hostNames", ((PhysicalInfrastructureMapping) infrastructureMapping).getHostNames());
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMappingWinRm) {
      validateInfraMapping(infrastructureMapping, fromYaml);
      keyValuePairs.put(
          "loadBalancerId", ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getLoadBalancerId());
      keyValuePairs.put("hostNames", ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getHostNames());
      keyValuePairs.put("winRmConnectionAttributes",
          ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getWinRmConnectionAttributes());
    } else if (infrastructureMapping instanceof CodeDeployInfrastructureMapping) {
      CodeDeployInfrastructureMapping codeDeployInfrastructureMapping =
          (CodeDeployInfrastructureMapping) infrastructureMapping;
      if (codeDeployInfrastructureMapping.getRegion() != null) {
        keyValuePairs.put("region", codeDeployInfrastructureMapping.getRegion());
      } else {
        fieldsToRemove.add("region");
      }
      keyValuePairs.put("applicationName", codeDeployInfrastructureMapping.getApplicationName());
      keyValuePairs.put("deploymentGroup", codeDeployInfrastructureMapping.getDeploymentGroup());
      keyValuePairs.put("deploymentConfig", codeDeployInfrastructureMapping.getDeploymentConfig());
      if (codeDeployInfrastructureMapping.getHostNameConvention() != null) {
        keyValuePairs.put("hostNameConvention", codeDeployInfrastructureMapping.getHostNameConvention());
      } else {
        fieldsToRemove.add("hostNameConvention");
      }
    } else if (infrastructureMapping instanceof AwsAmiInfrastructureMapping) {
      AwsAmiInfrastructureMapping awsAmiInfrastructureMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;
      if (awsAmiInfrastructureMapping.getRegion() != null) {
        keyValuePairs.put("region", awsAmiInfrastructureMapping.getRegion());
      } else {
        fieldsToRemove.add("region");
      }
      keyValuePairs.put("autoScalingGroupName", awsAmiInfrastructureMapping.getAutoScalingGroupName());
      if (awsAmiInfrastructureMapping.getClassicLoadBalancers() != null) {
        keyValuePairs.put("classicLoadBalancers", awsAmiInfrastructureMapping.getClassicLoadBalancers());
      }
      if (awsAmiInfrastructureMapping.getTargetGroupArns() != null) {
        keyValuePairs.put("targetGroupArns", awsAmiInfrastructureMapping.getTargetGroupArns());
      }
      if (awsAmiInfrastructureMapping.getStageClassicLoadBalancers() != null) {
        keyValuePairs.put("stageClassicLoadBalancers", awsAmiInfrastructureMapping.getStageClassicLoadBalancers());
      }
      if (awsAmiInfrastructureMapping.getStageTargetGroupArns() != null) {
        keyValuePairs.put("stageTargetGroupArns", awsAmiInfrastructureMapping.getStageTargetGroupArns());
      }
      if (awsAmiInfrastructureMapping.getHostNameConvention() != null) {
        keyValuePairs.put("hostNameConvention", awsAmiInfrastructureMapping.getHostNameConvention());
      } else {
        fieldsToRemove.add("hostNameConvention");
      }
    } else if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      PcfInfrastructureMapping pcfInfrastructureMapping = (PcfInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(pcfInfrastructureMapping, fromYaml);
      handlePcfInfraMapping(keyValuePairs, pcfInfrastructureMapping);
    }
    if (computeProviderSetting != null) {
      keyValuePairs.put("computeProviderName", computeProviderSetting.getName());
    }
    wingsPersistence.updateFields(
        infrastructureMapping.getClass(), infrastructureMapping.getUuid(), keyValuePairs, fieldsToRemove);
    InfrastructureMapping updatedInfraMapping = get(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());

    boolean isRename = !updatedInfraMapping.getName().equals(savedInfraMapping.getName());
    yamlPushService.pushYamlChangeSet(savedInfraMapping.getAccountId(), savedInfraMapping, updatedInfraMapping,
        Type.UPDATE, infrastructureMapping.isSyncFromGit(), isRename);

    return updatedInfraMapping;
  }

  @Override
  public InfrastructureMapping update(InfrastructureMapping infrastructureMapping) {
    return update(infrastructureMapping, false);
  }

  private List<String> getUniqueHostNames(PhysicalInfrastructureMappingBase physicalInfrastructureMapping) {
    List<String> hostNames = physicalInfrastructureMapping.getHostNames()
                                 .stream()
                                 .map(String::trim)
                                 .filter(StringUtils::isNotEmpty)
                                 .distinct()
                                 .collect(toList());
    if (hostNames.isEmpty()) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER).addParam("args", "Host names must not be empty");
    }
    return hostNames;
  }

  /**
   * This method gets the default name, checks if another entry exists with the same name, if exists, it parses and
   * extracts the chartVersion and creates a name with the next chartVersion.
   *
   * @param infraMapping
   */
  private void setAutoPopulatedName(InfrastructureMapping infraMapping) {
    String name = EntityNameValidator.getMappedString(infraMapping.getDefaultName());
    String escapedString = Pattern.quote(name);

    // We need to check if the name exists in case of auto generate, if it exists, we need to add a suffix to the name.
    PageRequest<InfrastructureMapping> pageRequest = aPageRequest()
                                                         .addFilter("appId", Operator.EQ, infraMapping.getAppId())
                                                         .addFilter("envId", Operator.EQ, infraMapping.getEnvId())
                                                         .addFilter("name", Operator.STARTS_WITH, escapedString)
                                                         .build();
    PageResponse<InfrastructureMapping> response = wingsPersistence.query(InfrastructureMapping.class, pageRequest);

    // If an entry exists with the given default name get the next revision number
    if (isNotEmpty(response)) {
      name = Util.getNameWithNextRevision(
          response.getResponse().stream().map(InfrastructureMapping::getName).collect(toList()), name);
    }

    infraMapping.setName(name);
  }

  private void validateEcsInfraMapping(EcsInfrastructureMapping infraMapping) {
    if (isNotEmpty(infraMapping.getProvisionerId())) {
      return;
    }
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck("SettingAttribute", settingAttribute, USER);
    String clusterName = infraMapping.getClusterName();
    String region = infraMapping.getRegion();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(app.getAccountId())
                                          .withAppId(app.getUuid())
                                          .withEnvId(infraMapping.getEnvId())
                                          .withInfrastructureMappingId(infraMapping.getUuid())
                                          .build();
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

  private void validateGcpInfraMapping(GcpKubernetesInfrastructureMapping infraMapping) {
    if (isNotEmpty(infraMapping.getProvisionerId())) {
      return;
    }
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck("SettingAttribute", settingAttribute, USER);
    String clusterName = infraMapping.getClusterName();
    String namespace = infraMapping.getNamespace();
    validateNamespace(namespace);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(app.getAccountId())
                                          .withAppId(app.getUuid())
                                          .withEnvId(infraMapping.getEnvId())
                                          .withInfrastructureMappingId(infraMapping.getUuid())
                                          .build();
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

  private void validateAzureInfraMapping(AzureInfrastructureMapping infraMapping) {
    if (isEmpty(infraMapping.getComputeProviderType()) || !infraMapping.getComputeProviderType().equals(AZURE.name())) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Compute Provider type is empty or not correct for Azure Infra mapping.");
    }

    if (isEmpty(infraMapping.getSubscriptionId())) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Subscription Id must not be empty for Azure Infra mapping.");
    }

    if (isEmpty(infraMapping.getDeploymentType())
        || (!infraMapping.getDeploymentType().equals(SSH.name())
               && !infraMapping.getDeploymentType().equals(WINRM.name()))) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Deployment type must not be empty and must be one of SSH/WINRM for Azure Infra mapping.");
    }

    if (isEmpty(infraMapping.getInfraMappingType())) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Infra mapping type must not be empty for Azure Infra mapping.");
    }
  }

  private void validateNamespace(String namespace) {
    try {
      new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
    } catch (Exception e) {
      throw new InvalidArgumentsException(Pair.of("Namespace", namespace + " is an invalid name"), e);
    }
  }

  private void validateAzureKubernetesInfraMapping(AzureKubernetesInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck("SettingAttribute", settingAttribute, USER);
    String clusterName = infraMapping.getClusterName();
    String subscriptionId = infraMapping.getSubscriptionId();
    String resourceGroup = infraMapping.getResourceGroup();
    String namespace = infraMapping.getNamespace();
    validateNamespace(namespace);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(app.getAccountId())
                                          .withAppId(app.getUuid())
                                          .withEnvId(infraMapping.getEnvId())
                                          .withInfrastructureMappingId(infraMapping.getUuid())
                                          .build();
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
    validateNamespace(namespace);

    List<EncryptedDataDetail> encryptionDetails =
        (infraMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name()))
        ? emptyList()
        : secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    Application app = appService.get(infraMapping.getAppId());
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(app.getAccountId())
                                          .withAppId(app.getUuid())
                                          .withEnvId(infraMapping.getEnvId())
                                          .withInfrastructureMappingId(infraMapping.getUuid())
                                          .build();
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
    notNullCheck("ComputeProviderSettingAttribute", settingAttribute);

    settingAttribute = settingsService.get(infraMapping.getWinRmConnectionAttributes());
    notNullCheck("WinRmConnectionAttributes", settingAttribute);
  }

  private void validatePcfInfrastructureMapping(PcfInfrastructureMapping infraMapping) {
    if (StringUtils.isBlank(infraMapping.getOrganization()) || StringUtils.isBlank(infraMapping.getSpace())) {
      logger.error("For PCFInfraMapping, Org and Space value cant be null");
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER).addParam("args", "Host names must be unique");
    }

    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck("ComputeProviderSettingAttribute", settingAttribute, USER);
  }

  private void validateAwsLambdaInfrastructureMapping(AwsLambdaInfraStructureMapping lambdaInfraStructureMapping) {
    if (lambdaInfraStructureMapping.getVpcId() != null) {
      if (lambdaInfraStructureMapping.getSubnetIds().isEmpty()) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "At least one subnet must be provided");
      }
      if (lambdaInfraStructureMapping.getSecurityGroupIds().isEmpty()) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "At least one security group must be provided");
      }
    }
  }

  @Override
  public void delete(String appId, String infraMappingId) {
    delete(appId, infraMappingId, false, false);
  }

  @Override
  public void deleteByYamlGit(String appId, String infraMappingId, boolean syncFromGit) {
    delete(appId, infraMappingId, false, syncFromGit);
  }

  private void delete(String appId, String infraMappingId, boolean forceDelete, boolean syncFromGit) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    if (infrastructureMapping == null) {
      return;
    }

    if (!forceDelete) {
      ensureSafeToDelete(appId, infraMappingId);
    }

    String accountId = appService.getAccountIdByAppId(infrastructureMapping.getAppId());
    yamlPushService.pushYamlChangeSet(accountId, infrastructureMapping, null, Type.DELETE, syncFromGit, false);

    prune(appId, infraMappingId);
  }

  private void prune(String appId, String infraMappingId) {
    PruneEntityJob.addDefaultJob(
        jobScheduler, InfrastructureMapping.class, appId, infraMappingId, ofSeconds(5), ofSeconds(15));

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
  public void pruneByInfrastructureProvisioner(String appId, String infrastructureProvisionerId) {
    List<Key<InfrastructureMapping>> keys =
        wingsPersistence.createQuery(InfrastructureMapping.class)
            .filter(InfrastructureMapping.APP_ID_KEY, appId)
            .filter(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureProvisionerId)
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

  @Override
  public void ensureSafeToDelete(@NotEmpty String appId, @NotEmpty String infraMappingId) {
    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest().withLimit(UNLIMITED).addFilter("appId", Operator.EQ, appId).build())
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
                        && infraMappingId.equals(workflowPhase.getInfraMappingId()));
              }
              return false;
            })
            .map(Workflow::getName)
            .collect(toList());

    if (!referencingWorkflowNames.isEmpty()) {
      throw new InvalidRequestException(
          format("Service Infrastructure %s is in use by %s %s [%s].", infraMappingId, referencingWorkflowNames.size(),
              plural("workflow", referencingWorkflowNames.size()), Joiner.on(", ").join(referencingWorkflowNames)),
          USER);
    }
  }

  @Override
  public void deleteByServiceTemplate(String appId, String serviceTemplateId) {
    List<Key<InfrastructureMapping>> keys = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                .filter("appId", appId)
                                                .filter("serviceTemplateId", serviceTemplateId)
                                                .asKeyList();
    keys.forEach(key -> delete(appId, (String) key.getId(), true, false));
  }

  @Override
  public Map<String, Object> getInfraMappingStencils(String appId) {
    return stencilPostProcessor
        .postProcess(Lists.newArrayList(InfrastructureMappingType.values()), appId, Maps.newHashMap())
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
              secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null),
              new PageRequest<>())
          .getResponse();
    } else if (infrastructureMapping instanceof AzureInfrastructureMapping) {
      AzureInfrastructureMapping azureInfraMapping = (AzureInfrastructureMapping) infrastructureMapping;
      SettingAttribute computeProviderSetting = settingsService.get(azureInfraMapping.getComputeProviderSettingId());
      notNullCheck("Compute Provider", computeProviderSetting);
      return azureHelperService.listHosts(azureInfraMapping, computeProviderSetting,
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null),
          new PageRequest<>());
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

  private AwsConfig validateAndGetAwsConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AwsConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "InvalidConfiguration");
    }
    return (AwsConfig) computeProviderSetting.getValue();
  }

  private AzureConfig validateAndGetAzureConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof AzureConfig)) {
      throw new WingsException(INVALID_ARGUMENT).addParam("args", "No cloud provider exist or not of type Azure");
    }
    return (AzureConfig) computeProviderSetting.getValue();
  }

  @Override
  public List<String> listClusters(String appId, String deploymentType, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    String type = computeProviderSetting.getValue().getType();
    if (AWS.name().equals(type)) {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsEcsHelperServiceManager.listClusters(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
    } else if (GCP.name().equals(type)) {
      GcpInfrastructureProvider infrastructureProvider =
          (GcpInfrastructureProvider) getInfrastructureProviderByComputeProviderType(GCP.name());
      return infrastructureProvider.listClusterNames(computeProviderSetting,
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null));
    }
    return emptyList();
  }

  @Override
  public List<String> listRegions(String appId, String deploymentType, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      try {
        AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
        return awsEc2HelperServiceManager.listRegions(
            awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), appId);
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
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
      return infrastructureProvider.listIAMInstanceRoles(computeProviderSetting, appId);
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
      try {
        AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
        return awsEc2HelperServiceManager.listTags(
            awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
    }
    return Collections.emptySet();
  }

  @Override
  public Set<String> listAzureTags(String appId, String computeProviderId, String subscriptionId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AZURE.name().equals(computeProviderSetting.getValue().getType())) {
      try {
        AzureConfig azureConfig = validateAndGetAzureConfig(computeProviderSetting);
        return azureHelperService.listTagsBySubscription(
            subscriptionId, azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null));
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
    }
    return Collections.emptySet();
  }

  @Override
  public Set<String> listAzureResourceGroups(String appId, String computeProviderId, String subscriptionId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AZURE.name().equals(computeProviderSetting.getValue().getType())) {
      try {
        AzureConfig azureConfig = validateAndGetAzureConfig(computeProviderSetting);
        return azureHelperService.listResourceGroups(
            azureConfig, secretManager.getEncryptionDetails(azureConfig, null, null), subscriptionId);
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
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
      return infrastructureProvider.listAutoScalingGroups(computeProviderSetting, region, appId);
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
      return infrastructureProvider.listTargetGroups(computeProviderSetting, region, null, appId);
    }
    return emptyMap();
  }

  @Override
  public Map<String, String> listAllRoles(String appId, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsIamHelperServiceManager.listIamRoles(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), appId);
    }
    return Collections.emptyMap();
  }

  @Override
  public List<String> listVPC(String appId, String computeProviderId, String region) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      try {
        AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
        return awsEc2HelperServiceManager.listVPCs(
            awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
    }
    return emptyList();
  }

  public List<String> listOrganizationsForPcf(String appId, String computeProviderId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "InvalidConfiguration");
    }
    return pcfHelperService.listOrganizations((PcfConfig) computeProviderSetting.getValue());
  }

  @Override
  public List<String> listSpacesForPcf(String appId, String computeProviderId, String organization) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "InvalidConfiguration");
    }
    return pcfHelperService.listSpaces((PcfConfig) computeProviderSetting.getValue(), organization);
  }

  @Override
  public List<String> lisRouteMapsForPcf(String appId, String computeProviderId, String organization, String spaces) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "InvalidConfiguration");
    }

    return pcfHelperService.listRoutes((PcfConfig) computeProviderSetting.getValue(), organization, spaces);
  }

  @Override
  public List<String> listSecurityGroups(String appId, String computeProviderId, String region, List<String> vpcIds) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      try {
        AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
        return awsEc2HelperServiceManager.listSGs(
            awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, vpcIds, appId);
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
    }
    return emptyList();
  }

  @Override
  public List<String> listSubnets(String appId, String computeProviderId, String region, List<String> vpcIds) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      try {
        AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
        return awsEc2HelperServiceManager.listSubnets(
            awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, vpcIds, appId);
      } catch (Exception e) {
        logger.warn(Misc.getMessage(e), e);
        throw new InvalidRequestException(Misc.getMessage(e), USER);
      }
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
      return infrastructureProvider.listLoadBalancers(computeProviderSetting, Regions.US_EAST_1.getName(), appId)
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
  public Map<String, String> listElasticLoadBalancers(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (infrastructureMapping instanceof EcsInfrastructureMapping
        || infrastructureMapping instanceof AwsInfrastructureMapping) {
      String region = infrastructureMapping instanceof EcsInfrastructureMapping
          ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
          : ((AwsInfrastructureMapping) infrastructureMapping).getRegion();

      return ((AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name()))
          .listElasticBalancers(computeProviderSetting, region, appId)
          .stream()
          .collect(toMap(s -> s, s -> s));
    }
    return Collections.emptyMap();
  }

  @Override
  public Map<String, String> listNetworkLoadBalancers(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (infrastructureMapping instanceof EcsInfrastructureMapping
        || infrastructureMapping instanceof AwsInfrastructureMapping) {
      String region = infrastructureMapping instanceof EcsInfrastructureMapping
          ? ((EcsInfrastructureMapping) infrastructureMapping).getRegion()
          : ((AwsInfrastructureMapping) infrastructureMapping).getRegion();

      return ((AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name()))
          .listNetworkBalancers(computeProviderSetting, region, appId)
          .stream()
          .collect(toMap(s -> s, s -> s));
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
      return infrastructureProvider.listClassicLoadBalancers(computeProviderSetting, region, appId);
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
          computeProviderSetting, Regions.US_EAST_1.getName(), loadBalancerName, appId);
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
      return infrastructureProvider.listTargetGroups(computeProviderSetting, region, loadbalancerName, appId);
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
        secretManager.getEncryptionDetails((EncryptableSetting) hostConnectionSetting.getValue(), null, null);
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(hostConnectionSetting.getAccountId())
                                          .withAppId(validationRequest.getAppId())
                                          .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                          .build();
    return delegateProxyFactory.get(HostValidationService.class, syncTaskContext)
        .validateHost(validationRequest.getHostNames(), hostConnectionSetting, encryptionDetails,
            validationRequest.getExecutionCredential());
  }

  @Override
  public List<String> listElasticLoadBalancer(String accessKey, char[] secretKey, String region, String accountId) {
    AwsInfrastructureProvider infrastructureProvider =
        (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
    return infrastructureProvider.listClassicLoadBalancers(accessKey, secretKey, region);
  }

  @Override
  public List<String> listCodeDeployApplicationNames(String computeProviderId, String region, String appId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsCodeDeployHelperServiceManager.listApplications(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
    }
    return ImmutableList.of();
  }

  @Override
  public List<String> listCodeDeployDeploymentGroups(
      String computeProviderId, String region, String applicationName, String appId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsCodeDeployHelperServiceManager.listDeploymentGroups(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, applicationName, appId);
    }
    return ImmutableList.of();
  }

  @Override
  public List<String> listCodeDeployDeploymentConfigs(String computeProviderId, String region, String appId) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (AWS.name().equals(computeProviderSetting.getValue().getType())) {
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      return awsCodeDeployHelperServiceManager.listDeploymentConfiguration(
          awsConfig, secretManager.getEncryptionDetails(awsConfig, appId, null), region, appId);
    }
    return ImmutableList.of();
  }

  @Override
  public List<String> listHostDisplayNames(String appId, String infraMappingId, String workflowExecutionId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Infra Mapping was deleted", infrastructureMapping, USER);
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
    List<String> hostDisplayNames = new ArrayList<>();
    if (infrastructureMapping instanceof PhysicalInfrastructureMappingBase) {
      return ((PhysicalInfrastructureMappingBase) infrastructureMapping).getHostNames();
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMapping;
      AwsInfrastructureProvider infrastructureProvider =
          (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
      SettingAttribute computeProviderSetting =
          settingsService.get(awsInfrastructureMapping.getComputeProviderSettingId());
      notNullCheck("Compute Provider", computeProviderSetting);
      List<Host> hosts =
          infrastructureProvider
              .listHosts(awsInfrastructureMapping, computeProviderSetting,
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
    } else if (infrastructureMapping instanceof AzureInfrastructureMapping) {
      AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infrastructureMapping;
      SettingAttribute computeProviderSetting =
          settingsService.get(azureInfrastructureMapping.getComputeProviderSettingId());
      notNullCheck("Compute Provider", computeProviderSetting);

      // Get VMs
      List<VirtualMachine> vms = azureHelperService.listVms(azureInfrastructureMapping, computeProviderSetting,
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null), null);
      hostDisplayNames = vms.stream().map(vm -> vm.name()).collect(Collectors.toList());
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

    String kubernetesName;
    String controllerNamePrefix;
    if (isNotBlank(serviceNameExpression)) {
      controllerNamePrefix = KubernetesConvention.normalize(evaluator.substitute(serviceNameExpression, context));
    } else {
      controllerNamePrefix =
          KubernetesConvention.getControllerNamePrefix(app.getName(), service.getName(), env.getName());
    }
    kubernetesName = isStatefulSet ? KubernetesConvention.getKubernetesServiceName(controllerNamePrefix)
                                   : (controllerNamePrefix + DASH + "0");

    if (containerInfraMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directInfraMapping =
          (DirectKubernetesInfrastructureMapping) containerInfraMapping;
      settingAttribute = (directInfraMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name()))
          ? aSettingAttribute().withValue(directInfraMapping.createKubernetesConfig()).build()
          : settingsService.get(directInfraMapping.getComputeProviderSettingId());
      namespace = directInfraMapping.getNamespace();

      containerServiceName = kubernetesName;
    } else {
      settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
      clusterName = containerInfraMapping.getClusterName();
      if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
        namespace = containerInfraMapping.getNamespace();
        containerServiceName = kubernetesName;
      } else if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
        namespace = containerInfraMapping.getNamespace();
        subscriptionId = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId();
        resourceGroup = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup();
        containerServiceName = kubernetesName;

      } else if (containerInfraMapping instanceof EcsInfrastructureMapping) {
        region = ((EcsInfrastructureMapping) containerInfraMapping).getRegion();
        containerServiceName = (isNotBlank(serviceNameExpression)
                                       ? Misc.normalizeExpression(evaluator.substitute(serviceNameExpression, context))
                                       : EcsConvention.getTaskFamily(app.getName(), service.getName(), env.getName()))
            + EcsConvention.DELIMITER + "0";
      }
    }
    notNullCheck("SettingAttribute might have been deleted", settingAttribute, USER);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(app.getAccountId())
                                          .withAppId(app.getUuid())
                                          .withEnvId(infrastructureMapping.getEnvId())
                                          .withInfrastructureMappingId(infraMappingId)
                                          .build();
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
      infraTypes.put(KUBERNETES,
          asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
      if (featureFlagService.isEnabled(FeatureName.HELM, accountId)) {
        infraTypes.put(HELM,
            asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
      }
      infraTypes.put(SSH, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.AWS_CODEDEPLOY) {
      infraTypes.put(AWS_CODEDEPLOY, asList(SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.AWS_LAMBDA) {
      infraTypes.put(AWS_LAMBDA, asList(SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.AMI) {
      infraTypes.put(AMI, asList(SettingVariableTypes.AWS));
    } else if (artifactType == ArtifactType.IIS || artifactType == ArtifactType.IIS_APP
        || artifactType == ArtifactType.IIS_VirtualDirectory) {
      infraTypes.put(WINRM,
          asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
    } else if (artifactType == ArtifactType.PCF) {
      infraTypes.put(PCF, asList(SettingVariableTypes.PCF));
    } else {
      infraTypes.put(
          SSH, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
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
