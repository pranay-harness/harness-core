package software.wings.helpers.ext.container;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import org.mongodb.morphia.Key;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.GcpKubernetesCluster;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.ContainerInfo.Status;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsEcrHelperServiceManager;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/6/18.
 */
@Singleton
public class ContainerDeploymentManagerHelper {
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EcrService ecrService;
  @Inject private EcrClassicService ecrClassicService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private AwsEcrHelperServiceManager awsEcrHelperServiceManager;

  public List<InstanceStatusSummary> getInstanceStatusSummaryFromContainerInfoList(
      List<ContainerInfo> containerInfos, ServiceTemplateElement serviceTemplateElement) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    if (isNotEmpty(containerInfos)) {
      for (ContainerInfo containerInfo : containerInfos) {
        HostElement hostElement = aHostElement()
                                      .withHostName(containerInfo.getHostName())
                                      .withIp(containerInfo.getIp())
                                      .withEc2Instance(containerInfo.getEc2Instance())
                                      .build();
        InstanceElement instanceElement = anInstanceElement()
                                              .withUuid(containerInfo.getContainerId())
                                              .withDockerId(containerInfo.getContainerId())
                                              .withHostName(containerInfo.getHostName())
                                              .withHost(hostElement)
                                              .withServiceTemplateElement(serviceTemplateElement)
                                              .withDisplayName(containerInfo.getContainerId())
                                              .withPodName(containerInfo.getPodName())
                                              .withWorkloadName(containerInfo.getWorkloadName())
                                              .withEcsContainerDetails(containerInfo.getEcsContainerDetails())
                                              .build();
        ExecutionStatus status =
            containerInfo.getStatus() == Status.SUCCESS ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
        instanceStatusSummaries.add(
            anInstanceStatusSummary().withStatus(status).withInstanceElement(instanceElement).build());
      }
    }
    return instanceStatusSummaries;
  }

  public List<InstanceStatusSummary> getInstanceStatusSummaries(
      ExecutionContext context, List<ContainerInfo> containerInfos) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    ServiceElement serviceElement = phaseElement.getServiceElement();
    String serviceId = phaseElement.getServiceElement().getUuid();
    String appId = context.getAppId();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams.getEnv().getUuid();

    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0);
    ServiceTemplateElement serviceTemplateElement = aServiceTemplateElement()
                                                        .withUuid(serviceTemplateKey.getId().toString())
                                                        .withServiceElement(serviceElement)
                                                        .build();

    return getInstanceStatusSummaryFromContainerInfoList(containerInfos, serviceTemplateElement);
  }

  public ContainerServiceParams getContainerServiceParams(
      ContainerInfrastructureMapping containerInfraMapping, String containerServiceName, ExecutionContext context) {
    String clusterName = containerInfraMapping.getClusterName();
    SettingAttribute settingAttribute;
    String namespace = null;
    String region = null;
    String resourceGroup = null;
    String subscriptionId = null;
    String masterUrl = null;
    settingAttribute = settingsService.get(containerInfraMapping.getComputeProviderSettingId());
    if (containerInfraMapping instanceof DirectKubernetesInfrastructureMapping) {
      namespace = containerInfraMapping.getNamespace();
    } else if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
      namespace = containerInfraMapping.getNamespace();
      masterUrl = ((GcpKubernetesInfrastructureMapping) containerInfraMapping).getMasterUrl();
    } else if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
      subscriptionId = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId();
      resourceGroup = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup();
      namespace = containerInfraMapping.getNamespace();
      masterUrl = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getMasterUrl();
    } else if (containerInfraMapping instanceof EcsInfrastructureMapping) {
      region = ((EcsInfrastructureMapping) containerInfraMapping).getRegion();
    }
    Validator.notNullCheck("SettingAttribute", settingAttribute);

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), containerInfraMapping.getAppId(), null);
    return ContainerServiceParams.builder()
        .settingAttribute(settingAttribute)
        .containerServiceName(containerServiceName)
        .encryptionDetails(encryptionDetails)
        .clusterName(clusterName)
        .namespace(context.renderExpression(namespace))
        .region(region)
        .subscriptionId(subscriptionId)
        .resourceGroup(resourceGroup)
        .masterUrl(masterUrl)
        .build();
  }

  public K8sClusterConfig getK8sClusterConfig(ContainerInfrastructureMapping containerInfraMapping) {
    SettingAttribute settingAttribute;
    AzureKubernetesCluster azureKubernetesCluster = null;
    GcpKubernetesCluster gcpKubernetesCluster = null;
    String namespace = null;
    String clusterName = null;
    if (containerInfraMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directInfraMapping =
          (DirectKubernetesInfrastructureMapping) containerInfraMapping;
      settingAttribute = settingsService.get(directInfraMapping.getComputeProviderSettingId());
      namespace = directInfraMapping.getNamespace();
      clusterName = settingAttribute.getName();
    } else {
      settingAttribute = settingsService.get(containerInfraMapping.getComputeProviderSettingId());
      if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
        gcpKubernetesCluster =
            GcpKubernetesCluster.builder().clusterName(containerInfraMapping.getClusterName()).build();
        namespace = containerInfraMapping.getNamespace();
        clusterName = gcpKubernetesCluster.getClusterName();
      } else if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
        azureKubernetesCluster =
            AzureKubernetesCluster.builder()
                .subscriptionId(((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId())
                .resourceGroup(((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup())
                .name(containerInfraMapping.getClusterName())
                .build();
        namespace = containerInfraMapping.getNamespace();
        clusterName = azureKubernetesCluster.getName();
      }
    }
    Validator.notNullCheck("SettingAttribute", settingAttribute);

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), containerInfraMapping.getAppId(), null);

    return K8sClusterConfig.builder()
        .cloudProvider(settingAttribute.getValue())
        .cloudProviderEncryptionDetails(encryptionDetails)
        .azureKubernetesCluster(azureKubernetesCluster)
        .gcpKubernetesCluster(gcpKubernetesCluster)
        .clusterName(clusterName)
        .namespace(namespace)
        .build();
  }
}
