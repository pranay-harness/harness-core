package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.allChecks;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;
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
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.common.InfrastructureConstants.INFRA_KUBERNETES_INFRAID_EXPRESSION;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_DESIRED_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MAX_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_MIN_INSTANCES;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_AMI_ASG_NAME;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.KubernetesConvention.DASH;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Tag;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.mongodb.DuplicateKeyException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SweepingOutput;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.EntityNameValidator;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.queue.Queue;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import io.harness.validation.Create;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.annotation.BlueprintProcessor;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.AccountEvent;
import software.wings.beans.AccountEventType;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsAmiInfrastructureMapping.AwsAmiInfrastructureMappingKeys;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.FeatureName;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
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
import software.wings.beans.SyncTaskContext;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.infrastructure.Host;
import software.wings.common.InfrastructureConstants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.container.ContainerMasterUrlHelper;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsCodeDeployHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEc2HelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsEcsHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsIamHelperServiceManager;
import software.wings.service.intfc.aws.manager.AwsRoute53HelperServiceManager;
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
import software.wings.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
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
@Slf4j
public class InfrastructureMappingServiceImpl implements InfrastructureMappingService {
  private static final String COMPUTE_PROVIDER_SETTING_ID_KEY = "computeProviderSettingId";
  private static final Integer REFERENCED_ENTITIES_TO_SHOW = 10;

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
  @Inject private AwsRoute53HelperServiceManager awsRoute53HelperServiceManager;
  @Inject private AwsAsgHelperServiceManager awsAsgHelperServiceManager;
  @Inject private YamlPushService yamlPushService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private TriggerService triggerService;
  @Inject private PipelineService pipelineService;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private ContainerMasterUrlHelper containerMasterUrlHelper;
  @Inject private ServiceTemplateHelper serviceTemplateHelper;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private SweepingOutputService sweepingOutputService;

  @Inject private Queue<PruneEvent> pruneQueue;

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

  @Override
  public void validateInfraMapping(@Valid InfrastructureMapping infraMapping, boolean fromYaml) {
    if (fromYaml) {
      logger.info("Ignore validation for InfraMapping created from yaml");
      return;
    }
    if (isNotEmpty(infraMapping.getProvisionerId())) {
      return;
    }

    if (infraMapping instanceof AwsInfrastructureMapping) {
      validateAwsInfraMapping((AwsInfrastructureMapping) infraMapping);
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
      if (gcpKubernetesInfrastructureMapping.getProvisionerId() == null) {
        gcpKubernetesInfrastructureMapping.setMasterUrl(containerMasterUrlHelper.fetchMasterUrl(
            gcpKubernetesInfrastructureMapping, getGcpContainerServiceParams(gcpKubernetesInfrastructureMapping),
            getSyncTaskContext(gcpKubernetesInfrastructureMapping)));
      }
    }

    if (infraMapping instanceof AzureKubernetesInfrastructureMapping) {
      AzureKubernetesInfrastructureMapping azureKubernetesInfrastructureMapping =
          (AzureKubernetesInfrastructureMapping) infraMapping;
      validateAzureKubernetesInfraMapping(azureKubernetesInfrastructureMapping);
      if (azureKubernetesInfrastructureMapping.getProvisionerId() == null) {
        azureKubernetesInfrastructureMapping.setMasterUrl(containerMasterUrlHelper.fetchMasterUrl(
            azureKubernetesInfrastructureMapping, getAzureContainerServiceParams(azureKubernetesInfrastructureMapping),
            getSyncTaskContext(azureKubernetesInfrastructureMapping)));
      }
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

    if (infraMapping instanceof AwsLambdaInfraStructureMapping) {
      validateAwsLambdaInfrastructureMapping((AwsLambdaInfraStructureMapping) infraMapping);
    }

    if (infraMapping instanceof AwsAmiInfrastructureMapping) {
      validateAwsAmiInfrastructureMapping((AwsAmiInfrastructureMapping) infraMapping);
    }
  }

  @VisibleForTesting
  public void validateProvisionerConfig(InfrastructureMapping infraMapping) {
    if (isEmpty(infraMapping.getProvisionerId())) {
      return;
    }
    if (isEmpty(infraMapping.getBlueprints())) {
      throw new InvalidRequestException("Blueprints can't be empty with Provisioner", USER);
    }
    BlueprintProcessor.validateKeys(infraMapping, infraMapping.getBlueprints());
  }

  private String fetchReleaseName(InfrastructureMapping infraMapping) {
    Service service = serviceResourceService.get(infraMapping.getAppId(), infraMapping.getServiceId(), false);
    if (service == null) {
      return null;
    }

    String releaseName = null;

    if (infraMapping instanceof AzureKubernetesInfrastructureMapping
        || infraMapping instanceof DirectKubernetesInfrastructureMapping
        || infraMapping instanceof GcpKubernetesInfrastructureMapping) {
      releaseName = ((ContainerInfrastructureMapping) infraMapping).getReleaseName();

      if (isBlank(releaseName)) {
        releaseName = INFRA_KUBERNETES_INFRAID_EXPRESSION;
      }
    }

    return releaseName;
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
    boolean infraRefactor =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infraMapping.getAccountId());
    if (infraMapping.isAutoPopulate()) {
      setAutoPopulatedName(infraMapping);
    }

    SettingAttribute computeProviderSetting = settingsService.get(infraMapping.getComputeProviderSettingId());

    if (!infraRefactor) {
      ServiceTemplate serviceTemplate =
          serviceTemplateService.get(infraMapping.getAppId(), infraMapping.getServiceTemplateId());
      notNullCheck("Service Template", serviceTemplate, USER);
      infraMapping.setServiceId(serviceTemplate.getServiceId());
    }

    if (computeProviderSetting != null) {
      infraMapping.setComputeProviderName(computeProviderSetting.getName());
    }

    if (infraMapping instanceof ContainerInfrastructureMapping) {
      ((ContainerInfrastructureMapping) infraMapping).setReleaseName(fetchReleaseName(infraMapping));
    }

    validateInfraMapping(infraMapping, fromYaml);
    InfrastructureMapping savedInfraMapping;
    try {
      savedInfraMapping = wingsPersistence.saveAndGet(InfrastructureMapping.class, infraMapping);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(ex.getMessage());
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause() instanceof DuplicateKeyException) {
        throw new DuplicateFieldException(e.getCause().getMessage());
      } else {
        throw new UnexpectedException(ExceptionUtils.getMessage(e));
      }
    }

    String accountId = appService.getAccountIdByAppId(infraMapping.getAppId());
    if (!infraRefactor) {
      yamlPushService.pushYamlChangeSet(
          accountId, null, savedInfraMapping, Type.CREATE, infraMapping.isSyncFromGit(), false);
    }
    if (!savedInfraMapping.isSample()) {
      eventPublishHelper.publishAccountEvent(
          accountId, AccountEvent.builder().accountEventType(AccountEventType.INFRA_MAPPING_ADDED).build());
    }
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

    if (isNotEmpty(infrastructureMapping.getInfrastructureDefinitionId())) {
      keyValuePairs.put(
          InfrastructureMappingKeys.infrastructureDefinitionId, infrastructureMapping.getInfrastructureDefinitionId());
    } else {
      fieldsToRemove.add(InfrastructureMappingKeys.infrastructureDefinitionId);
    }

    if (isNotEmpty(infrastructureMapping.getProvisionerId())) {
      keyValuePairs.put(InfrastructureMapping.PROVISIONER_ID_KEY, infrastructureMapping.getProvisionerId());
    } else {
      fieldsToRemove.add(InfrastructureMapping.PROVISIONER_ID_KEY);
    }

    if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureMapping.getAccountId())) {
      if (isNotEmpty(infrastructureMapping.getBlueprints())) {
        keyValuePairs.put(InfrastructureMappingKeys.blueprints, infrastructureMapping.getBlueprints());
      } else {
        fieldsToRemove.add(InfrastructureMappingKeys.blueprints);
      }
    }

    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      EcsInfrastructureMapping ecsInfrastructureMapping = (EcsInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(ecsInfrastructureMapping, fromYaml);
      handleEcsInfraMapping(keyValuePairs, fieldsToRemove, ecsInfrastructureMapping);

    } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
          (DirectKubernetesInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(directKubernetesInfrastructureMapping, fromYaml);
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
      if (isNotEmpty(gcpKubernetesInfrastructureMapping.getClusterName())) {
        keyValuePairs.put("clusterName", gcpKubernetesInfrastructureMapping.getClusterName());
      } else {
        fieldsToRemove.add("clusterName");
      }
      if (isNotEmpty(gcpKubernetesInfrastructureMapping.getNamespace())) {
        keyValuePairs.put("namespace", gcpKubernetesInfrastructureMapping.getNamespace());
      } else {
        fieldsToRemove.add("namespace");
      }
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
      validateInfraMapping(azureInfrastructureMapping, fromYaml);
      keyValuePairs.put("subscriptionId", azureInfrastructureMapping.getSubscriptionId());
      keyValuePairs.put("resourceGroup", azureInfrastructureMapping.getResourceGroup());
      keyValuePairs.put("tags", azureInfrastructureMapping.getTags());
      keyValuePairs.put("usePublicDns", azureInfrastructureMapping.isUsePublicDns());
      if (DeploymentType.SSH.name().equals(infrastructureMapping.getDeploymentType())) {
        keyValuePairs.put("hostConnectionAttrs", azureInfrastructureMapping.getHostConnectionAttrs());
      }
      if (DeploymentType.WINRM.name().equals(infrastructureMapping.getDeploymentType())) {
        keyValuePairs.put("winRmConnectionAttributes", azureInfrastructureMapping.getWinRmConnectionAttributes());
      }

      if (!StringUtils.equals(((AzureInfrastructureMapping) savedInfraMapping).getWinRmConnectionAttributes(),
              azureInfrastructureMapping.getWinRmConnectionAttributes())) {
        getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType())
            .updateHostConnAttrs(infrastructureMapping, azureInfrastructureMapping.getWinRmConnectionAttributes());
      }

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
      if (awsInfrastructureMapping.getHostConnectionType() != null) {
        keyValuePairs.put("hostConnectionType", awsInfrastructureMapping.getHostConnectionType());
      } else {
        fieldsToRemove.add("hostConnectionType");
      }
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
      if (lambdaInfraStructureMapping.getVpcId() == null) {
        if (isNotEmpty(lambdaInfraStructureMapping.getSubnetIds())
            || isNotEmpty(lambdaInfraStructureMapping.getSecurityGroupIds())) {
          throw new InvalidRequestException("Subnets or Security Groups can't be added without any VPC.");
        }
        fieldsToRemove.addAll(Arrays.asList("vpcId", "subnetIds", "securityGroupIds"));
      } else {
        keyValuePairs.put("vpcId", lambdaInfraStructureMapping.getVpcId());
        keyValuePairs.put("subnetIds", lambdaInfraStructureMapping.getSubnetIds());
        keyValuePairs.put("securityGroupIds", lambdaInfraStructureMapping.getSecurityGroupIds());
      }
      if (lambdaInfraStructureMapping.getRole() != null) {
        keyValuePairs.put("role", lambdaInfraStructureMapping.getRole());
      } else {
        fieldsToRemove.add("role");
      }

      // BLUEPRINT
      if (featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureMapping.getAccountId())) {
        Map<String, Object> blueprints = infrastructureMapping.getBlueprints();
        if (blueprints == null) {
          fieldsToRemove.add(InfrastructureMappingKeys.blueprints);
        } else {
          keyValuePairs.put(InfrastructureMappingKeys.blueprints, blueprints);
        }
      }

    } else if (infrastructureMapping instanceof PhysicalInfrastructureMapping) {
      PhysicalInfrastructureMapping physicalInfrastructureMapping =
          (PhysicalInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(physicalInfrastructureMapping, fromYaml);
      if (isNotEmpty(physicalInfrastructureMapping.hosts())) {
        keyValuePairs.put("hosts", physicalInfrastructureMapping.hosts());
      } else {
        fieldsToRemove.add("hosts");
      }
      if (isNotEmpty(physicalInfrastructureMapping.getHostNames())) {
        keyValuePairs.put("hostNames", physicalInfrastructureMapping.getHostNames());
      } else {
        fieldsToRemove.add("hostNames");
      }
      if (isNotEmpty(physicalInfrastructureMapping.getLoadBalancerId())) {
        keyValuePairs.put("loadBalancerId", physicalInfrastructureMapping.getLoadBalancerId());
      } else {
        fieldsToRemove.add("loadBalancerId");
      }
    } else if (infrastructureMapping instanceof PhysicalInfrastructureMappingWinRm) {
      validateInfraMapping(infrastructureMapping, fromYaml);
      keyValuePairs.put(
          "loadBalancerId", ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getLoadBalancerId());
      keyValuePairs.put("hostNames", ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getHostNames());

      if (!StringUtils.equals(((PhysicalInfrastructureMappingWinRm) savedInfraMapping).getWinRmConnectionAttributes(),
              ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getWinRmConnectionAttributes())) {
        getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType())
            .updateHostConnAttrs(infrastructureMapping,
                ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getWinRmConnectionAttributes());

        keyValuePairs.put("winRmConnectionAttributes",
            ((PhysicalInfrastructureMappingWinRm) infrastructureMapping).getWinRmConnectionAttributes());
      }
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
      AwsAmiInfrastructureMapping savedAwsAmiInfrastructureMapping = (AwsAmiInfrastructureMapping) savedInfraMapping;
      validateInfraMapping(awsAmiInfrastructureMapping, fromYaml);
      if (awsAmiInfrastructureMapping.getRegion() != null) {
        keyValuePairs.put("region", awsAmiInfrastructureMapping.getRegion());
      } else {
        fieldsToRemove.add("region");
      }
      if (awsAmiInfrastructureMapping.getHostNameConvention() != null) {
        keyValuePairs.put("hostNameConvention", awsAmiInfrastructureMapping.getHostNameConvention());
      } else {
        fieldsToRemove.add("hostNameConvention");
      }
      if (AmiDeploymentType.AWS_ASG.equals(savedAwsAmiInfrastructureMapping.getAmiDeploymentType())) {
        if (awsAmiInfrastructureMapping.getAutoScalingGroupName() != null) {
          keyValuePairs.put("autoScalingGroupName", awsAmiInfrastructureMapping.getAutoScalingGroupName());
        } else {
          fieldsToRemove.add("autoScalingGroupName");
        }
        if (awsAmiInfrastructureMapping.getClassicLoadBalancers() != null) {
          keyValuePairs.put("classicLoadBalancers", awsAmiInfrastructureMapping.getClassicLoadBalancers());
        } else {
          fieldsToRemove.add("classicLoadBalancers");
        }
        if (awsAmiInfrastructureMapping.getTargetGroupArns() != null) {
          keyValuePairs.put("targetGroupArns", awsAmiInfrastructureMapping.getTargetGroupArns());
        } else {
          fieldsToRemove.add("targetGroupArns");
        }
        if (awsAmiInfrastructureMapping.getStageClassicLoadBalancers() != null) {
          keyValuePairs.put("stageClassicLoadBalancers", awsAmiInfrastructureMapping.getStageClassicLoadBalancers());
        } else {
          fieldsToRemove.add("stageClassicLoadBalancers");
        }
        if (awsAmiInfrastructureMapping.getStageTargetGroupArns() != null) {
          keyValuePairs.put("stageTargetGroupArns", awsAmiInfrastructureMapping.getStageTargetGroupArns());
        } else {
          fieldsToRemove.add("stageTargetGroupArns");
        }
      } else if (AmiDeploymentType.SPOTINST.equals(savedAwsAmiInfrastructureMapping.getAmiDeploymentType())) {
        if (isNotEmpty(awsAmiInfrastructureMapping.getSpotinstCloudProvider())) {
          keyValuePairs.put(AwsAmiInfrastructureMappingKeys.spotinstCloudProvider,
              awsAmiInfrastructureMapping.getSpotinstCloudProvider());
        } else {
          fieldsToRemove.add(AwsAmiInfrastructureMappingKeys.spotinstCloudProvider);
        }
        if (isNotEmpty(awsAmiInfrastructureMapping.getSpotinstElastiGroupJson())) {
          keyValuePairs.put(AwsAmiInfrastructureMappingKeys.spotinstElastiGroupJson,
              awsAmiInfrastructureMapping.getSpotinstElastiGroupJson());
        } else {
          fieldsToRemove.add(AwsAmiInfrastructureMappingKeys.spotinstElastiGroupJson);
        }
      } else {
        throw new InvalidRequestException(format(
            "Unrecognized Ami deployment type: [%s]", savedAwsAmiInfrastructureMapping.getAmiDeploymentType().name()));
      }
    } else if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      PcfInfrastructureMapping pcfInfrastructureMapping = (PcfInfrastructureMapping) infrastructureMapping;
      validateInfraMapping(pcfInfrastructureMapping, fromYaml);
      handlePcfInfraMapping(keyValuePairs, pcfInfrastructureMapping);
    }
    if (computeProviderSetting != null) {
      keyValuePairs.put("computeProviderName", computeProviderSetting.getName());
    }

    if (infrastructureMapping instanceof ContainerInfrastructureMapping) {
      String releaseName = fetchReleaseName(infrastructureMapping);
      if (isNotBlank(releaseName)) {
        keyValuePairs.put("releaseName", releaseName);
      }
    }

    wingsPersistence.updateFields(
        infrastructureMapping.getClass(), infrastructureMapping.getUuid(), keyValuePairs, fieldsToRemove);
    InfrastructureMapping updatedInfraMapping = get(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());

    boolean isRename = !updatedInfraMapping.getName().equals(savedInfraMapping.getName());

    boolean infraRefactor =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, savedInfraMapping.getAccountId());
    if (!infraRefactor) {
      yamlPushService.pushYamlChangeSet(savedInfraMapping.getAccountId(), savedInfraMapping, updatedInfraMapping,
          Type.UPDATE, infrastructureMapping.isSyncFromGit(), isRename);
    }

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
      name = Utils.getNameWithNextRevision(
          response.getResponse().stream().map(InfrastructureMapping::getName).collect(toList()), name);
    }

    infraMapping.setName(name);
  }

  @VisibleForTesting
  public void validateEcsInfraMapping(EcsInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck("SettingAttribute", settingAttribute, USER);
    String clusterName = infraMapping.getClusterName();
    String region = infraMapping.getRegion();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    SyncTaskContext syncTaskContext = getSyncTaskContext(infraMapping);
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptionDetails)
                                                        .clusterName(clusterName)
                                                        .region(region)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  @VisibleForTesting
  public void validateAwsInfraMapping(AwsInfrastructureMapping infraMapping) {
    if (infraMapping.isProvisionInstances()) {
      if (isEmpty(infraMapping.getAutoScalingGroupName())) {
        throw new WingsException(INVALID_ARGUMENT)
            .addParam("args", "Auto Scaling group must not be empty when provision instances is true.");
      }
      if (infraMapping.isSetDesiredCapacity() && infraMapping.getDesiredCapacity() <= 0) {
        throw new WingsException(INVALID_ARGUMENT).addParam("args", "Desired count must be greater than zero.");
      }
    } else {
      if (infraMapping.getAwsInstanceFilter() == null) {
        throw new WingsException(INVALID_ARGUMENT)
            .addParam("args", "Instance filter must not be null when provision instances is false.");
      }
    }
  }

  @VisibleForTesting
  public void validateGcpInfraMapping(GcpKubernetesInfrastructureMapping infraMapping) {
    String clusterName = infraMapping.getClusterName();
    String namespace = infraMapping.getNamespace();
    if (isEmpty(clusterName)) {
      throw new InvalidRequestException("Cluster name can't be empty");
    }
    if (isEmpty(namespace)) {
      throw new InvalidRequestException("Namespace can't be empty");
    }
    if (DeploymentType.KUBERNETES.name().equals(infraMapping.getDeploymentType())) {
      if (isEmpty(infraMapping.getReleaseName())) {
        throw new InvalidRequestException("Release name can't be empty");
      }
    }
    KubernetesHelperService.validateNamespace(namespace);

    ContainerServiceParams containerServiceParams = getGcpContainerServiceParams(infraMapping);

    SyncTaskContext syncTaskContext = getSyncTaskContext(infraMapping);

    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(ExceptionUtils.getMessage(e), e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  private ContainerServiceParams getGcpContainerServiceParams(GcpKubernetesInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck(format("No cloud provider found with given id : [%s]", infraMapping.getComputeProviderSettingId()),
        settingAttribute, USER);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue());
    return ContainerServiceParams.builder()
        .settingAttribute(settingAttribute)
        .encryptionDetails(encryptionDetails)
        .clusterName(infraMapping.getClusterName())
        .namespace(infraMapping.getNamespace())
        .build();
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

    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infraMapping, null, infraMapping.getServiceId());
    if (!(SSH.equals(deploymentType) || WINRM.equals(deploymentType))) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Deployment type must not be empty and must be one of SSH/WINRM for Azure Infra mapping.");
    }

    if (isEmpty(infraMapping.getInfraMappingType())) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Infra mapping type must not be empty for Azure Infra mapping.");
    }
  }

  private void validateAzureKubernetesInfraMapping(AzureKubernetesInfrastructureMapping infraMapping) {
    if (isNotEmpty(infraMapping.getProvisionerId())) {
      return;
    }
    KubernetesHelperService.validateNamespace(infraMapping.getNamespace());

    ContainerServiceParams containerServiceParams = getAzureContainerServiceParams(infraMapping);
    SyncTaskContext syncTaskContext = getSyncTaskContext(infraMapping);

    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(ExceptionUtils.getMessage(e), e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  private ContainerServiceParams getAzureContainerServiceParams(AzureKubernetesInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    notNullCheck("SettingAttribute", settingAttribute, USER);
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue());

    return ContainerServiceParams.builder()
        .settingAttribute(settingAttribute)
        .encryptionDetails(encryptionDetails)
        .clusterName(infraMapping.getClusterName())
        .subscriptionId(infraMapping.getSubscriptionId())
        .resourceGroup(infraMapping.getResourceGroup())
        .namespace(infraMapping.getNamespace())
        .build();
  }

  private SyncTaskContext getSyncTaskContext(InfrastructureMapping infraMapping) {
    Application app = appService.get(infraMapping.getAppId());
    return SyncTaskContext.builder()
        .accountId(app.getAccountId())
        .appId(app.getUuid())
        .envId(infraMapping.getEnvId())
        .infrastructureMappingId(infraMapping.getUuid())
        .infraStructureDefinitionId(infraMapping.getInfrastructureDefinitionId())
        .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
        .build();
  }

  private void validateDirectKubernetesInfraMapping(DirectKubernetesInfrastructureMapping infraMapping) {
    SettingAttribute settingAttribute = settingsService.get(infraMapping.getComputeProviderSettingId());
    String namespace = infraMapping.getNamespace();

    if (isNotEmpty(infraMapping.getProvisionerId())) {
      return;
    }

    KubernetesHelperService.validateNamespace(namespace);

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

    SyncTaskContext syncTaskContext = getSyncTaskContext(infraMapping);
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder()
                                                        .settingAttribute(settingAttribute)
                                                        .encryptionDetails(encryptionDetails)
                                                        .namespace(namespace)
                                                        .build();
    try {
      delegateProxyFactory.get(ContainerService.class, syncTaskContext).validate(containerServiceParams);
    } catch (Exception e) {
      logger.warn(ExceptionUtils.getMessage(e), e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
    }
  }

  @Override
  public InfrastructureMapping get(String appId, String infraMappingId) {
    return wingsPersistence.getWithAppId(InfrastructureMapping.class, appId, infraMappingId);
  }

  void handlePcfInfraMapping(Map<String, Object> keyValuePairs, PcfInfrastructureMapping pcfInfrastructureMapping) {
    keyValuePairs.put("organization", pcfInfrastructureMapping.getOrganization());
    keyValuePairs.put("space", pcfInfrastructureMapping.getSpace());
    keyValuePairs.put("tempRouteMap",
        pcfInfrastructureMapping.getTempRouteMap() == null ? Collections.EMPTY_LIST
                                                           : pcfInfrastructureMapping.getTempRouteMap());
    keyValuePairs.put("routeMaps",
        pcfInfrastructureMapping.getRouteMaps() == null ? Collections.EMPTY_LIST
                                                        : pcfInfrastructureMapping.getRouteMaps());
  }

  void handleEcsInfraMapping(Map<String, Object> keyValuePairs, Set<String> fieldsToRemove,
      EcsInfrastructureMapping ecsInfrastructureMapping) {
    if (isNotEmpty(ecsInfrastructureMapping.getClusterName())) {
      keyValuePairs.put("clusterName", ecsInfrastructureMapping.getClusterName());
    } else {
      fieldsToRemove.add("clusterName");
    }

    if (isNotEmpty(ecsInfrastructureMapping.getRegion())) {
      keyValuePairs.put("region", ecsInfrastructureMapping.getRegion());
    } else {
      fieldsToRemove.add("region");
    }

    if (isNotEmpty(ecsInfrastructureMapping.getLaunchType())) {
      keyValuePairs.put("launchType", ecsInfrastructureMapping.getLaunchType());
    } else {
      fieldsToRemove.add("launchType");
    }
    keyValuePairs.put("assignPublicIp", ecsInfrastructureMapping.isAssignPublicIp());
    keyValuePairs.put(
        "vpcId", ecsInfrastructureMapping.getVpcId() == null ? EMPTY : ecsInfrastructureMapping.getVpcId());
    keyValuePairs.put("subnetIds",
        ecsInfrastructureMapping.getSubnetIds() == null ? Collections.EMPTY_LIST
                                                        : ecsInfrastructureMapping.getSubnetIds());
    keyValuePairs.put("securityGroupIds",
        ecsInfrastructureMapping.getSecurityGroupIds() == null ? Collections.EMPTY_LIST
                                                               : ecsInfrastructureMapping.getSecurityGroupIds());
    keyValuePairs.put("executionRole",
        ecsInfrastructureMapping.getExecutionRole() == null ? EMPTY : ecsInfrastructureMapping.getExecutionRole());
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

  @VisibleForTesting
  public void validateAwsLambdaInfrastructureMapping(AwsLambdaInfraStructureMapping infraMapping) {
    if (StringUtils.isEmpty(infraMapping.getRegion())) {
      throw new InvalidRequestException("Region is mandatory");
    }
    if (StringUtils.isEmpty(infraMapping.getRole())) {
      throw new InvalidRequestException("IAM Role is mandatory");
    }
  }

  @VisibleForTesting
  public void validateAwsAmiInfrastructureMapping(AwsAmiInfrastructureMapping infrastructureMapping) {
    if (isEmpty(infrastructureMapping.getRegion())) {
      throw new InvalidRequestException("Region is mandatory");
    }
    if (AmiDeploymentType.AWS_ASG.equals(infrastructureMapping.getAmiDeploymentType())
        && isEmpty(infrastructureMapping.getAutoScalingGroupName())) {
      throw new InvalidRequestException("Auto Scaling Group is mandatory");
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
    boolean infraRefactor = featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, accountId);

    if (!infraRefactor) {
      yamlPushService.pushYamlChangeSet(accountId, infrastructureMapping, null, Type.DELETE, syncFromGit, false);
    }

    prune(appId, infraMappingId);
  }

  private void prune(String appId, String infraMappingId) {
    pruneQueue.send(new PruneEvent(InfrastructureMapping.class, appId, infraMappingId));
    wingsPersistence.delete(InfrastructureMapping.class, appId, infraMappingId);
  }

  @Override
  public void pruneByEnvironment(String appId, String envId) {
    List<InfrastructureMapping> infrastructureMappings = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                             .filter(InfrastructureMapping.APP_ID_KEY, appId)
                                                             .filter("envId", envId)
                                                             .project(InfrastructureMapping.APP_ID_KEY, true)
                                                             .project(InfrastructureMapping.ENV_ID_KEY, true)
                                                             .project(InfrastructureMapping.NAME_KEY, true)
                                                             .project(InfrastructureMapping.ID_KEY, true)
                                                             .asList();
    for (InfrastructureMapping infrastructureMapping : infrastructureMappings) {
      prune(appId, infrastructureMapping.getUuid());
      auditServiceHelper.reportDeleteForAuditing(appId, infrastructureMapping);
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
    PruneEntityListener.pruneDescendingEntities(
        services, descending -> descending.pruneByInfrastructureMapping(appId, infraMappingId));
  }

  @Override
  public void ensureSafeToDelete(@NotEmpty String appId, @NotEmpty String infraMappingId) {
    List<String> referencingWorkflowNames =
        workflowService.obtainWorkflowNamesReferencedByServiceInfrastructure(appId, infraMappingId);

    if (!referencingWorkflowNames.isEmpty()) {
      throw new InvalidRequestException(
          format("Service Infrastructure %s is referenced by %s %s [%s].", infraMappingId,
              referencingWorkflowNames.size(), plural("workflow", referencingWorkflowNames.size()),
              Joiner.on(", ").join(referencingWorkflowNames)),
          USER);
    }

    List<String> refPipelines = pipelineService.obtainPipelineNamesReferencedByTemplatedEntity(appId, infraMappingId);
    if (isNotEmpty(refPipelines)) {
      throw new InvalidRequestException(
          format("Service Infrastructure is referenced by %d %s [%s] as a workflow variable.", refPipelines.size(),
              plural("pipeline", refPipelines.size()), Joiner.on(", ").join(refPipelines)),
          USER);
    }

    List<String> refTriggers = triggerService.obtainTriggerNamesReferencedByTemplatedEntityId(appId, infraMappingId);
    if (isNotEmpty(refTriggers)) {
      throw new InvalidRequestException(
          format("Service Infrastructure is referenced by %d %s [%s] as a workflow variable.", refTriggers.size(),
              plural("trigger", refTriggers.size()), Joiner.on(", ").join(refTriggers)),
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
      if (EmptyPredicate.isNotEmpty(pyInfraMapping.hosts())) {
        return pyInfraMapping.hosts();
      }
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
                     .withInfraDefinitionId(pyInfraMapping.getInfrastructureDefinitionId())
                     .withHostConnAttr(pyInfraMapping.getHostConnectionAttrs())
                     .withServiceTemplateId(serviceTemplateHelper.fetchServiceTemplateId(pyInfraMapping))
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
                     .withInfraDefinitionId(pyInfraMappingWinRm.getInfrastructureDefinitionId())
                     .withWinrmConnAttr(pyInfraMappingWinRm.getWinRmConnectionAttributes())
                     .withServiceTemplateId(serviceTemplateHelper.fetchServiceTemplateId(pyInfraMappingWinRm))
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
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(azureInfraMapping, null, azureInfraMapping.getServiceId());

      return azureHelperService.listHosts(azureInfraMapping, computeProviderSetting,
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null),
          deploymentType);
    } else {
      throw new InvalidRequestException(
          "Unsupported infrastructure mapping: " + infrastructureMapping.getClass().getName());
    }
  }

  private List<ServiceInstance> syncHostsAndUpdateInstances(
      InfrastructureMapping infrastructureMapping, List<Host> hosts) {
    InfrastructureProvider infrastructureProvider =
        getInfrastructureProviderByComputeProviderType(infrastructureMapping.getComputeProviderType());
    ServiceTemplate serviceTemplate = serviceTemplateHelper.fetchServiceTemplate(infrastructureMapping);

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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
      }
    }
    return emptyList();
  }

  @Override
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
  public String createRoute(String appId, String computeProviderId, String organization, String spaces, String host,
      String domain, String path, boolean tcpRoute, boolean useRandomPort, String port) {
    SettingAttribute computeProviderSetting = settingsService.get(computeProviderId);
    notNullCheck("Compute Provider", computeProviderSetting);

    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT, USER).addParam("args", "InvalidConfiguration");
    }

    Integer portNum = StringUtils.isBlank(port) ? null : Integer.parseInt(port);
    return pcfHelperService.createRoute((PcfConfig) computeProviderSetting.getValue(), organization, spaces, host,
        domain, path, tcpRoute, useRandomPort, portNum);
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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
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
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
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

  String extractRegionFromInfraMapping(InfrastructureMapping infrastructureMapping) {
    String region = EMPTY;
    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      region = ((EcsInfrastructureMapping) infrastructureMapping).getRegion();
    } else if (infrastructureMapping instanceof AwsInfrastructureMapping) {
      region = ((AwsInfrastructureMapping) infrastructureMapping).getRegion();
    } else if (infrastructureMapping instanceof AwsAmiInfrastructureMapping) {
      region = ((AwsAmiInfrastructureMapping) infrastructureMapping).getRegion();
    }
    return region;
  }

  @Override
  public Map<String, String> listElasticLoadBalancers(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);
    String region = extractRegionFromInfraMapping(infrastructureMapping);
    if (isEmpty(region)) {
      return Collections.emptyMap();
    }

    return ((AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name()))
        .listElasticBalancers(computeProviderSetting, region, appId)
        .stream()
        .collect(toMap(s -> s, s -> s));
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
  public List<AwsRoute53HostedZoneData> listHostedZones(String appId, String infraMappingId) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    if (infrastructureMapping instanceof EcsInfrastructureMapping) {
      String region = ((EcsInfrastructureMapping) infrastructureMapping).getRegion();
      AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
      List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, appId, null);
      return awsRoute53HelperServiceManager.listHostedZones(awsConfig, encryptionDetails, region, appId);
    }
    return emptyList();
  }

  @Override
  public List<AwsElbListener> listListeners(String appId, String infraMappingId, String loadbalancerName) {
    InfrastructureMapping infrastructureMapping = get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);

    String region = extractRegionFromInfraMapping(infrastructureMapping);
    if (isEmpty(region)) {
      return Collections.emptyList();
    }

    AwsInfrastructureProvider infrastructureProvider =
        (AwsInfrastructureProvider) getInfrastructureProviderByComputeProviderType(AWS.name());
    return infrastructureProvider.listListeners(computeProviderSetting, region, loadbalancerName, appId);
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
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(hostConnectionSetting.getAccountId())
                                          .appId(validationRequest.getAppId())
                                          .envId(validationRequest.getEnvId())
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 3)
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
      if (infrastructureMapping.getProvisionerId() != null) {
        return ((PhysicalInfrastructureMappingBase) infrastructureMapping)
            .hosts()
            .stream()
            .map(Host::getHostName)
            .collect(Collectors.toList());
      }
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
          secretManager.getEncryptionDetails((EncryptableSetting) computeProviderSetting.getValue(), null, null));
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
        serviceResourceService.getWithDetails(infrastructureMapping.getAppId(), infrastructureMapping.getServiceId());

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

    DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, service, null);
    ContainerTask containerTask = serviceResourceService.getContainerTaskByDeploymentType(
        app.getUuid(), service.getUuid(), deploymentType.name());
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
      settingAttribute = settingsService.get(directInfraMapping.getComputeProviderSettingId());
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

    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder()
            .accountId(app.getAccountId())
            .appId(app.getUuid())
            .envId(infrastructureMapping.getEnvId())
            .infrastructureMappingId(infraMappingId)
            .infraStructureDefinitionId(infrastructureMapping.getInfrastructureDefinitionId())
            .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
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
      logger.warn(ExceptionUtils.getMessage(e), e);
      return "0";
    }
  }

  @Override
  public AwsAsgGetRunningCountData getAmiCurrentlyRunningInstanceCount(String infraMappingId, String appId) {
    AwsAmiInfrastructureMapping infrastructureMapping = (AwsAmiInfrastructureMapping) get(appId, infraMappingId);
    notNullCheck("Service Infrastructure", infrastructureMapping);
    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider", computeProviderSetting);
    String region = infrastructureMapping.getRegion();
    if (isEmpty(region)) {
      // case that could happen since we support dynamic infra for Ami Asg
      return AwsAsgGetRunningCountData.builder()
          .asgName(DEFAULT_AMI_ASG_NAME)
          .asgMin(DEFAULT_AMI_ASG_MIN_INSTANCES)
          .asgMax(DEFAULT_AMI_ASG_MAX_INSTANCES)
          .asgDesired(DEFAULT_AMI_ASG_DESIRED_INSTANCES)
          .build();
    }
    AwsConfig awsConfig = validateAndGetAwsConfig(computeProviderSetting);
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(awsConfig, appId, null);
    return awsAsgHelperServiceManager.getCurrentlyRunningInstanceCount(
        awsConfig, encryptionDetails, region, infraMappingId, appId);
  }

  @Override
  public Integer getPcfRunningInstances(String appId, String infraMappingId, String appNameExpression) {
    PcfInfrastructureMapping infrastructureMapping = (PcfInfrastructureMapping) get(appId, infraMappingId);
    notNullCheck("Inframapping Doesnt Exists", infrastructureMapping);

    Application app = appService.get(infrastructureMapping.getAppId());
    Environment env = envService.get(infrastructureMapping.getAppId(), infrastructureMapping.getEnvId(), false);
    Service service =
        serviceResourceService.getWithDetails(infrastructureMapping.getAppId(), infrastructureMapping.getServiceId());

    Map<String, Object> context = new HashMap<>();
    context.put("app", app);
    context.put("env", env);
    context.put("service", service);

    SettingAttribute computeProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    notNullCheck("Compute Provider Doesnt Exist", computeProviderSetting);

    if (!(computeProviderSetting.getValue() instanceof PcfConfig)) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "InvalidConfiguration, Needs Instance of PcfConfig");
    }

    appNameExpression = StringUtils.isNotBlank(appNameExpression)
        ? Misc.normalizeExpression(evaluator.substitute(appNameExpression, context))
        : EcsConvention.getTaskFamily(app.getName(), service.getName(), env.getName());

    return pcfHelperService.getRunningInstanceCount((PcfConfig) computeProviderSetting.getValue(),
        infrastructureMapping.getOrganization(), infrastructureMapping.getSpace(), appNameExpression);
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
  public List<InfrastructureMapping> getInfraMappingLinkedToInfraDefinition(String appId, String infraDefinitionId) {
    return wingsPersistence.createQuery(InfrastructureMapping.class)
        .field(InfrastructureMappingKeys.appId)
        .equal(appId)
        .field(InfrastructureMappingKeys.infrastructureDefinitionId)
        .equal(infraDefinitionId)
        .asList();
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
    Service service = serviceResourceService.getWithDetails(appId, serviceId);
    ArtifactType artifactType = service.getArtifactType();
    Map<DeploymentType, List<SettingVariableTypes>> infraTypes = new HashMap<>();

    if (service.getDeploymentType() != null) {
      boolean restrictInfraMappings = true;
      switch (service.getDeploymentType()) {
        case KUBERNETES:
          infraTypes.put(KUBERNETES,
              asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
          break;
        case HELM:
          infraTypes.put(HELM,
              asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
          break;
        case ECS:
          infraTypes.put(ECS, asList(SettingVariableTypes.AWS));
          break;
        case AWS_CODEDEPLOY:
          infraTypes.put(AWS_CODEDEPLOY, asList(SettingVariableTypes.AWS));
          break;
        case AWS_LAMBDA:
          infraTypes.put(AWS_LAMBDA, asList(SettingVariableTypes.AWS));
          break;
        case AMI:
          infraTypes.put(AMI, asList(SettingVariableTypes.AWS));
          break;
        case WINRM:
          infraTypes.put(WINRM,
              asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
          break;
        case PCF:
          infraTypes.put(PCF, asList(SettingVariableTypes.PCF));
          break;
        case SSH:
          if (ArtifactType.DOCKER.equals(artifactType)) {
            infraTypes.put(SSH, asList(SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS));
          } else {
            infraTypes.put(SSH,
                asList(
                    SettingVariableTypes.PHYSICAL_DATA_CENTER, SettingVariableTypes.AWS, SettingVariableTypes.AZURE));
          }
          break;
        default:
          restrictInfraMappings = false;
      }
      if (restrictInfraMappings) {
        return infraTypes;
      }
    }

    if (artifactType == ArtifactType.DOCKER) {
      infraTypes.put(ECS, asList(SettingVariableTypes.AWS));
      infraTypes.put(KUBERNETES,
          asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
      infraTypes.put(
          HELM, asList(SettingVariableTypes.GCP, SettingVariableTypes.AZURE, SettingVariableTypes.KUBERNETES_CLUSTER));
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

  @Override
  public List<InfrastructureMapping> listByComputeProviderId(String accountId, String computeProviderId) {
    return wingsPersistence.createQuery(InfrastructureMapping.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(COMPUTE_PROVIDER_SETTING_ID_KEY, computeProviderId)
        .asList(new FindOptions().limit(REFERENCED_ENTITIES_TO_SHOW));
  }

  @Override
  public List<String> fetchCloudProviderIds(String appId, List<String> infraMappingIds) {
    if (isNotEmpty(infraMappingIds)) {
      List<InfrastructureMapping> infrastructureMappings =
          wingsPersistence.createQuery(InfrastructureMapping.class)
              .project(InfrastructureMappingKeys.appId, true)
              .project(InfrastructureMappingKeys.computeProviderSettingId, true)
              .filter(InfrastructureMappingKeys.appId, appId)
              .field(InfrastructureMappingKeys.uuid)
              .in(infraMappingIds)
              .asList();
      return infrastructureMappings.stream()
          .map(InfrastructureMapping::getComputeProviderSettingId)
          .distinct()
          .collect(toList());
    }
    return new ArrayList<>();
  }

  @Override
  public List<InfrastructureMapping> listInfraMappings(String appId, String envId) {
    PageRequest pageRequest = PageRequestBuilder.aPageRequest()
                                  .addFilter(InfrastructureMappingKeys.appId, Operator.EQ, appId)
                                  .addFilter(InfrastructureMappingKeys.envId, Operator.EQ, envId)
                                  .build();
    return list(pageRequest);
  }

  @Override
  public void saveInfrastructureMappingToSweepingOutput(
      String appId, String workflowExecutionId, PhaseElement phaseElement, String infrastructureMappingId) {
    String phaseExecutionId = phaseElement.getPhaseExecutionIdForSweepingOutput();
    sweepingOutputService.save(SweepingOutputServiceImpl
                                   .prepareSweepingOutputBuilder(appId, null, workflowExecutionId, phaseExecutionId,
                                       null, SweepingOutput.Scope.PHASE)
                                   .name(InfrastructureConstants.PHASE_INFRA_MAPPING_KEY + phaseElement.getUuid())
                                   .output(KryoUtils.asDeflatedBytes(infrastructureMappingId))
                                   .build());
  }
}
