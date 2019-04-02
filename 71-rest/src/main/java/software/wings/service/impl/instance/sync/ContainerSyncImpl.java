package software.wings.service.impl.instance.sync;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.instance.sync.request.ContainerSyncRequest;
import software.wings.service.impl.instance.sync.response.ContainerSyncResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by brett on 9/6/17
 */
public class ContainerSyncImpl implements ContainerSync {
  private static final Logger logger = LoggerFactory.getLogger(ContainerSyncImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public ContainerSyncResponse getInstances(ContainerSyncRequest syncRequest) {
    List<ContainerInfo> result = Lists.newArrayList();
    for (ContainerDeploymentInfo containerDeploymentInfo :
        syncRequest.getFilter().getContainerDeploymentInfoCollection()) {
      try {
        InfrastructureMapping infrastructureMapping =
            infraMappingService.get(containerDeploymentInfo.getAppId(), containerDeploymentInfo.getInfraMappingId());
        if (!(infrastructureMapping instanceof ContainerInfrastructureMapping)) {
          String msg =
              "Unsupported infrastructure mapping type for containers :" + infrastructureMapping.getInfraMappingType();
          logger.error(msg);
          throw new WingsException(msg);
        }

        SettingAttribute settingAttribute;
        String clusterName = null;
        String region = null;
        String subscriptionId = null;
        String resourceGroup = null;
        ContainerInfrastructureMapping containerInfraMapping = (ContainerInfrastructureMapping) infrastructureMapping;
        settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
        clusterName = containerInfraMapping.getClusterName();
        if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
          subscriptionId = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId();
          resourceGroup = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup();
        } else if (containerInfraMapping instanceof EcsInfrastructureMapping) {
          region = ((EcsInfrastructureMapping) containerInfraMapping).getRegion();
        }
        Validator.notNullCheck("SettingAttribute", settingAttribute);

        List<EncryptedDataDetail> encryptionDetails =
            secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(),
                infrastructureMapping.getAppId(), containerDeploymentInfo.getWorkflowExecutionId());

        Application app = appService.get(infrastructureMapping.getAppId());

        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(app.getAccountId())
                                              .appId(app.getUuid())
                                              .envId(infrastructureMapping.getEnvId())
                                              .infrastructureMappingId(infrastructureMapping.getUuid())
                                              .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 2)
                                              .build();
        ContainerServiceParams containerServiceParams =
            ContainerServiceParams.builder()
                .settingAttribute(settingAttribute)
                .containerServiceName(containerDeploymentInfo.getContainerSvcName())
                .encryptionDetails(encryptionDetails)
                .clusterName(clusterName)
                .namespace(containerDeploymentInfo.getNamespace())
                .region(region)
                .subscriptionId(subscriptionId)
                .resourceGroup(resourceGroup)
                .build();

        result.addAll(delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                          .getContainerInfos(containerServiceParams));
      } catch (Exception ex) {
        logger.warn(
            "Error while getting instances for container {}", containerDeploymentInfo.getContainerSvcName(), ex);
        throw new WingsException(ErrorCode.GENERAL_ERROR, ex)
            .addParam("message",
                "Error while getting instances for container " + containerDeploymentInfo.getContainerSvcName());
      }
    }
    return ContainerSyncResponse.builder().containerInfoList(result).build();
  }

  @Override
  public ContainerSyncResponse getInstances(
      ContainerInfrastructureMapping containerInfraMapping, List<ContainerMetadata> containerMetadataList) {
    List<ContainerInfo> result = Lists.newArrayList();

    logger.info("getInstances() call for app {} , infraMapping {}", containerInfraMapping.getAppId(),
        containerInfraMapping.getUuid());

    for (ContainerMetadata containerMetadata : containerMetadataList) {
      try {
        logger.info("getInstances() call for app {} , infraMapping {} and containerSvcName {}",
            containerInfraMapping.getAppId(), containerInfraMapping.getUuid(), containerMetadata);

        ContainerServiceParams containerServiceParams = getContainerServiceParams(
            containerInfraMapping, containerMetadata.getContainerServiceName(), containerMetadata.getNamespace());
        Application app = appService.get(containerInfraMapping.getAppId());
        SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                              .accountId(app.getAccountId())
                                              .appId(app.getUuid())
                                              .envId(containerInfraMapping.getEnvId())
                                              .infrastructureMappingId(containerInfraMapping.getUuid())
                                              .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 2)
                                              .build();

        result.addAll(delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                          .getContainerInfos(containerServiceParams));
      } catch (Exception ex) {
        logger.warn(
            "Error while getting instances for container for appId {} and infraMappingId {} and containerSvcName {}",
            containerInfraMapping.getAppId(), containerInfraMapping.getUuid(), containerMetadata, ex);
        throw new WingsException(ErrorCode.GENERAL_ERROR, ex)
            .addParam("message",
                "Error while getting instances for container for appId " + containerInfraMapping.getAppId()
                    + " and infraMappingId " + containerInfraMapping.getUuid() + " and containerSvcName "
                    + containerMetadata);
      }
    }
    return ContainerSyncResponse.builder().containerInfoList(result).build();
  }

  @Override
  public Set<String> getControllerNames(
      ContainerInfrastructureMapping containerInfraMapping, Map<String, String> labels, String namespace) {
    ContainerServiceParams containerServiceParams = getContainerServiceParams(containerInfraMapping, null, namespace);

    Application app = appService.get(containerInfraMapping.getAppId());
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(app.getAccountId())
                                          .appId(app.getUuid())
                                          .envId(containerInfraMapping.getEnvId())
                                          .infrastructureMappingId(containerInfraMapping.getUuid())
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 2)
                                          .build();

    return delegateProxyFactory.get(ContainerService.class, syncTaskContext)
        .getControllerNames(containerServiceParams, labels);
  }

  private ContainerServiceParams getContainerServiceParams(
      ContainerInfrastructureMapping containerInfraMapping, String containerSvcName, String namespace) {
    SettingAttribute settingAttribute;
    String clusterName = null;
    String region = null;
    String resourceGroup = null;
    String subscriptionId = null;
    settingAttribute = settingsService.get(containerInfraMapping.getComputeProviderSettingId());
    clusterName = containerInfraMapping.getClusterName();
    if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
      subscriptionId = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId();
      resourceGroup = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup();
    } else if (containerInfraMapping instanceof EcsInfrastructureMapping) {
      region = ((EcsInfrastructureMapping) containerInfraMapping).getRegion();
    }
    Validator.notNullCheck("SettingAttribute", settingAttribute);

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), containerInfraMapping.getAppId(), null);

    return ContainerServiceParams.builder()
        .settingAttribute(settingAttribute)
        .containerServiceName(containerSvcName)
        .encryptionDetails(encryptionDetails)
        .clusterName(clusterName)
        .namespace(namespace)
        .region(region)
        .subscriptionId(subscriptionId)
        .resourceGroup(resourceGroup)
        .build();
  }
}
