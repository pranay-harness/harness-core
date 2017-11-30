package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;
import static software.wings.utils.KubernetesConvention.getReplicationControllerNamePrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.Service;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import software.wings.api.ContainerServiceElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ContainerService;
import software.wings.settings.SettingValue;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContainerServiceImpl implements ContainerService {
  @Inject private GkeClusterService gkeClusterService;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private AwsClusterService awsClusterService;

  @Override
  public Optional<Integer> getServiceDesiredCount(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerServiceElement containerServiceElement, String region) {
    if (isNotEmpty(containerServiceElement.getName())) {
      SettingValue value = settingAttribute.getValue();
      if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
        KubernetesConfig kubernetesConfig = value instanceof GcpConfig
            ? gkeClusterService.getCluster(settingAttribute, encryptedDataDetails,
                  containerServiceElement.getClusterName(), containerServiceElement.getNamespace())
            : (KubernetesConfig) value;
        ReplicationController replicationController = kubernetesContainerService.getController(
            kubernetesConfig, encryptedDataDetails, containerServiceElement.getName());
        if (replicationController != null) {
          return Optional.of(replicationController.getSpec().getReplicas());
        }
      } else if (value instanceof AwsConfig) {
        Optional<Service> service =
            awsClusterService
                .getServices(region, settingAttribute, encryptedDataDetails, containerServiceElement.getClusterName())
                .stream()
                .filter(svc -> svc.getServiceName().equals(containerServiceElement.getName()))
                .findFirst();
        if (service.isPresent()) {
          return Optional.of(service.get().getDesiredCount());
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCounts(SettingAttribute settingAttribute,
      List<EncryptedDataDetail> encryptedDataDetails, ContainerServiceElement containerServiceElement, String region) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    SettingValue value = settingAttribute.getValue();
    if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
      KubernetesConfig kubernetesConfig = settingAttribute.getValue() instanceof GcpConfig
          ? gkeClusterService.getCluster(settingAttribute, encryptedDataDetails,
                containerServiceElement.getClusterName(), containerServiceElement.getNamespace())
          : (KubernetesConfig) settingAttribute.getValue();
      ReplicationControllerList replicationControllers =
          kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails);
      if (replicationControllers != null) {
        String controllerNamePrefix =
            getReplicationControllerNamePrefixFromControllerName(containerServiceElement.getName());
        List<ReplicationController> activeOldReplicationControllers =
            replicationControllers.getItems()
                .stream()
                .filter(
                    c -> c.getMetadata().getName().startsWith(controllerNamePrefix) && c.getSpec().getReplicas() > 0)
                .collect(Collectors.toList());
        activeOldReplicationControllers.sort(
            Comparator.comparingInt(rc -> getRevisionFromControllerName(rc.getMetadata().getName())));
        activeOldReplicationControllers.forEach(
            rc -> result.put(rc.getMetadata().getName(), rc.getSpec().getReplicas()));
      }
    } else if (value instanceof AwsConfig) {
      String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceElement.getName());
      List<Service> activeOldServices =
          awsClusterService
              .getServices(region, settingAttribute, encryptedDataDetails, containerServiceElement.getClusterName())
              .stream()
              .filter(
                  service -> service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0)
              .collect(Collectors.toList());
      activeOldServices.sort(Comparator.comparingInt(service -> getRevisionFromServiceName(service.getServiceName())));
      activeOldServices.forEach(service -> result.put(service.getServiceName(), service.getDesiredCount()));
    }
    return result;
  }
}
