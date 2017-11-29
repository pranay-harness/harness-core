package software.wings.beans.command;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.awaitility.Awaitility.with;
import static software.wings.beans.container.ContainerTask.AdvancedType.JSON;
import static software.wings.beans.container.ContainerTask.AdvancedType.YAML;
import static software.wings.beans.container.ContainerTask.CONTAINER_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.ContainerTask.DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.ContainerTask.SECRET_NAME_PLACEHOLDER_REGEX;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.awaitility.core.ConditionTimeoutException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.KubernetesConvention;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(KubernetesSetupCommandUnit.class);

  @Transient
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  @Inject @Transient private transient GkeClusterService gkeClusterService;

  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;

  public KubernetesSetupCommandUnit() {
    super(CommandUnitType.KUBERNETES_SETUP);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected String executeInternal(SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, ContainerSetupParams containerSetupParams,
      Map<String, String> serviceVariables, ExecutionLogCallback executionLogCallback) {
    KubernetesSetupParams setupParams = (KubernetesSetupParams) containerSetupParams;

    KubernetesConfig kubernetesConfig;
    if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
      kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
    } else {
      kubernetesConfig = gkeClusterService.getCluster(
          cloudProviderSetting, encryptedDataDetails, clusterName, setupParams.getNamespace());
    }
    String lastReplicationControllerName =
        lastReplicationController(kubernetesConfig, setupParams.getRcNamePrefix(), encryptedDataDetails);

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;

    Map<String, String> serviceLabels =
        ImmutableMap.<String, String>builder()
            .put("app", KubernetesConvention.getLabelValue(setupParams.getAppName()))
            .put("service", KubernetesConvention.getLabelValue(setupParams.getServiceName()))
            .put("env", KubernetesConvention.getLabelValue(setupParams.getEnvName()))
            .build();

    Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                               .putAll(serviceLabels)
                                               .put("revision", Integer.toString(revision))
                                               .build();

    String kubernetesServiceName = KubernetesConvention.getKubernetesServiceName(setupParams.getRcNamePrefix());

    String secretName = KubernetesConvention.getKubernetesSecretName(
        kubernetesServiceName, setupParams.getImageDetails().getSourceName());

    kubernetesContainerService.createNamespaceIfNotExist(kubernetesConfig, emptyList());

    kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, emptyList(),
        createRegistrySecret(
            secretName, kubernetesConfig.getNamespace(), setupParams.getImageDetails(), executionLogCallback));

    String containerServiceName =
        fetchContainerServiceName(kubernetesConfig, setupParams.getRcNamePrefix(), encryptedDataDetails);

    ReplicationController rcDefinition = createReplicationControllerDefinition(setupParams.getContainerTask(),
        containerServiceName, controllerLabels, kubernetesConfig.getNamespace(), setupParams.getImageDetails(),
        secretName, serviceVariables, executionLogCallback);
    kubernetesContainerService.createController(kubernetesConfig, emptyList(), rcDefinition);

    String serviceClusterIP = null;
    String serviceLoadBalancerEndpoint = null;

    Service service = kubernetesContainerService.getService(kubernetesConfig, emptyList(), kubernetesServiceName);

    if (setupParams.getServiceType() != null && setupParams.getServiceType() != KubernetesServiceType.None) {
      Service serviceDefinition =
          createServiceDefinition(kubernetesServiceName, kubernetesConfig.getNamespace(), serviceLabels, setupParams);
      if (service != null) {
        // Keep the previous load balancer IP if it exists and a new one was not specified
        LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
        if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer
            && isEmpty(setupParams.getLoadBalancerIP()) && loadBalancer != null
            && !loadBalancer.getIngress().isEmpty()) {
          setupParams.setLoadBalancerIP(loadBalancer.getIngress().get(0).getIp());
          serviceDefinition = createServiceDefinition(
              kubernetesServiceName, kubernetesConfig.getNamespace(), serviceLabels, setupParams);
        }
      }
      service = kubernetesContainerService.createOrReplaceService(kubernetesConfig, emptyList(), serviceDefinition);
      serviceClusterIP = service.getSpec().getClusterIP();

      if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer) {
        serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(
            kubernetesConfig, service, setupParams.getLoadBalancerIP(), executionLogCallback);
      }
    } else if (service != null) {
      executionLogCallback.saveExecutionLog(
          "Kubernetes service type set to 'None'. Deleting existing service " + kubernetesServiceName, LogLevel.INFO);
      kubernetesContainerService.deleteService(kubernetesConfig, emptyList(), kubernetesServiceName);
    }

    executionLogCallback.saveExecutionLog("Cleaning up old versions", LogLevel.INFO);
    cleanup(kubernetesConfig, containerServiceName);

    String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();

    executionLogCallback.saveExecutionLog("Cluster Name: " + clusterName, LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Replication Controller Name: " + containerServiceName, LogLevel.INFO);
    executionLogCallback.saveExecutionLog("Service Name: " + kubernetesServiceName, LogLevel.INFO);
    if (isNotBlank(serviceClusterIP)) {
      executionLogCallback.saveExecutionLog("Service Cluster IP: " + serviceClusterIP, LogLevel.INFO);
    }
    if (isNotBlank(serviceLoadBalancerEndpoint)) {
      executionLogCallback.saveExecutionLog("Load Balancer Endpoint: " + serviceLoadBalancerEndpoint, LogLevel.INFO);
    }
    executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);

    return containerServiceName;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public static final class Builder extends ContainerSetupCommandUnit.Yaml.Builder {
      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      @Override
      protected Yaml getCommandUnitYaml() {
        return new KubernetesSetupCommandUnit.Yaml();
      }
    }
  }

  private String fetchContainerServiceName(
      KubernetesConfig kubernetesConfig, String rcNamePrefix, List<EncryptedDataDetail> encryptedDataDetails) {
    String lastReplicationControllerName =
        lastReplicationController(kubernetesConfig, rcNamePrefix, encryptedDataDetails);

    int revision = KubernetesConvention.getRevisionFromControllerName(lastReplicationControllerName) + 1;
    return KubernetesConvention.getReplicationControllerName(rcNamePrefix, revision);
  }

  private Secret createRegistrySecret(
      String secretName, String namespace, ImageDetails imageDetails, ExecutionLogCallback executionLogCallback) {
    String credentialData = String.format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
        imageDetails.getUsername(), imageDetails.getPassword());
    executionLogCallback.saveExecutionLog("Setting secret " + secretName, LogLevel.INFO);
    return new SecretBuilder()
        .withData(ImmutableMap.of(".dockercfg", new String(Base64.getEncoder().encode(credentialData.getBytes()))))
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType("kubernetes.io/dockercfg")
        .withKind("Secret")
        .build();
  }

  private String waitForLoadBalancerEndpoint(KubernetesConfig kubernetesConfig, Service service, String loadBalancerIP,
      ExecutionLogCallback executionLogCallback) {
    String loadBalancerEndpoint = null;
    String serviceName = service.getMetadata().getName();
    LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
    if (loadBalancer != null) {
      if (loadBalancer.getIngress().isEmpty()) {
        executionLogCallback.saveExecutionLog(
            "Waiting for service " + serviceName + " load balancer to be ready.", LogLevel.INFO);
        try {
          with().pollInterval(1, TimeUnit.SECONDS).await().atMost(60, TimeUnit.SECONDS).until(() -> {
            LoadBalancerStatus loadBalancerStatus =
                kubernetesContainerService.getService(kubernetesConfig, emptyList(), serviceName)
                    .getStatus()
                    .getLoadBalancer();
            boolean loadBalancerReady = !loadBalancerStatus.getIngress().isEmpty();
            if (loadBalancerReady && isNotEmpty(loadBalancerIP)) {
              loadBalancerReady = loadBalancerIP.equals(loadBalancerStatus.getIngress().get(0).getIp());
            }
            return loadBalancerReady;
          });
        } catch (ConditionTimeoutException e) {
          executionLogCallback.saveExecutionLog(
              String.format("Timed out waiting for service [%s] load balancer to be ready.", serviceName),
              LogLevel.INFO);
          return null;
        }
        loadBalancer = kubernetesContainerService.getService(kubernetesConfig, emptyList(), serviceName)
                           .getStatus()
                           .getLoadBalancer();
      }
      LoadBalancerIngress loadBalancerIngress = loadBalancer.getIngress().get(0);
      loadBalancerEndpoint = isNotEmpty(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname()
                                                                           : loadBalancerIngress.getIp();
    }
    executionLogCallback.saveExecutionLog(
        String.format("Service [%s] load balancer is ready with endpoint [%s].", serviceName, loadBalancerEndpoint),
        LogLevel.INFO);
    return loadBalancerEndpoint;
  }

  private String lastReplicationController(
      KubernetesConfig kubernetesConfig, String controllerNamePrefix, List<EncryptedDataDetail> encryptedDataDetails) {
    ReplicationControllerList replicationControllers =
        kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails);
    if (replicationControllers == null) {
      return null;
    }

    ReplicationController lastReplicationController = null;
    for (ReplicationController controller :
        replicationControllers.getItems()
            .stream()
            .filter(c
                -> c.getMetadata().getName().equals(controllerNamePrefix)
                    || c.getMetadata().getName().startsWith(controllerNamePrefix + KubernetesConvention.DOT))
            .collect(Collectors.toList())) {
      int revision = getRevisionFromControllerName(controller.getMetadata().getName());
      if (lastReplicationController == null
          || revision > getRevisionFromControllerName(lastReplicationController.getMetadata().getName())) {
        lastReplicationController = controller;
      }
    }
    return lastReplicationController != null ? lastReplicationController.getMetadata().getName() : null;
  }

  /**
   * Creates replication controller definition
   */
  private ReplicationController createReplicationControllerDefinition(ContainerTask containerTask,
      String replicationControllerName, Map<String, String> controllerLabels, String namespace,
      ImageDetails imageDetails, String secretName, Map<String, String> serviceVariables,
      ExecutionLogCallback executionLogCallback) {
    KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
    if (kubernetesContainerTask == null) {
      kubernetesContainerTask = new KubernetesContainerTask();
      ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
      kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
    }

    String containerName = KubernetesConvention.getContainerName(imageDetails.getName());

    String configTemplate;
    ContainerTask.AdvancedType type;
    if (isNotEmpty(kubernetesContainerTask.getAdvancedConfig())) {
      configTemplate = kubernetesContainerTask.getAdvancedConfig();
      type = kubernetesContainerTask.getAdvancedType();
    } else {
      configTemplate = kubernetesContainerTask.fetchYamlConfig();
      type = YAML;
    }

    String config =
        configTemplate
            .replaceAll(DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX, imageDetails.getName() + ":" + imageDetails.getTag())
            .replaceAll(CONTAINER_NAME_PLACEHOLDER_REGEX, containerName)
            .replaceAll(SECRET_NAME_PLACEHOLDER_REGEX, secretName);

    try {
      ReplicationController rc =
          type == JSON ? (ReplicationController) KubernetesHelper.loadJson(config) : KubernetesHelper.loadYaml(config);

      KubernetesHelper.setName(rc, replicationControllerName);
      KubernetesHelper.setNamespace(rc, namespace);
      KubernetesHelper.getOrCreateLabels(rc).putAll(controllerLabels);
      rc.getSpec().setSelector(controllerLabels);
      Map<String, String> labels = rc.getSpec().getTemplate().getMetadata().getLabels();
      if (labels == null) {
        labels = new HashMap<>();
        rc.getSpec().getTemplate().getMetadata().setLabels(labels);
      }
      rc.getSpec().getTemplate().getMetadata().getLabels().putAll(controllerLabels);
      rc.getSpec().setReplicas(0);

      // Set service variables as environment variables
      if (serviceVariables != null && !serviceVariables.isEmpty()) {
        Map<String, EnvVar> serviceEnvVars =
            serviceVariables.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> new EnvVarBuilder().withName(entry.getKey()).withValue(entry.getValue()).build()));
        for (Container container : rc.getSpec().getTemplate().getSpec().getContainers()) {
          Map<String, EnvVar> envVarsMap = new HashMap<>();
          if (container.getEnv() != null) {
            container.getEnv().forEach(envVar -> envVarsMap.put(envVar.getName(), envVar));
          }
          envVarsMap.putAll(serviceEnvVars);
          container.setEnv(new ArrayList<>(envVarsMap.values()));
        }
      }
      return rc;

    } catch (Exception e) {
      executionLogCallback.saveExecutionLog(e.getMessage(), LogLevel.ERROR);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "args", e.getMessage(), e);
    }
  }

  /**
   * Creates service definition
   */
  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(
      String serviceName, String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    ServiceSpecBuilder spec =
        new ServiceSpecBuilder().addToSelector(serviceLabels).withType(setupParams.getServiceType().name());

    if (setupParams.getServiceType() != KubernetesServiceType.ExternalName) {
      ServicePortBuilder servicePort = new ServicePortBuilder()
                                           .withProtocol(setupParams.getProtocol().name())
                                           .withPort(setupParams.getPort())
                                           .withNewTargetPort()
                                           .withIntVal(setupParams.getTargetPort())
                                           .endTargetPort();
      if (setupParams.getServiceType() == KubernetesServiceType.NodePort && setupParams.getNodePort() != null) {
        servicePort.withNodePort(setupParams.getNodePort());
      }
      spec.withPorts(ImmutableList.of(servicePort.build())); // TODO:: Allow more than one port

      if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer
          && isNotEmpty(setupParams.getLoadBalancerIP())) {
        spec.withLoadBalancerIP(setupParams.getLoadBalancerIP());
      }

      if (setupParams.getServiceType() == KubernetesServiceType.ClusterIP && isNotEmpty(setupParams.getClusterIP())) {
        spec.withClusterIP(setupParams.getClusterIP());
      }
    } else {
      // TODO:: fabric8 doesn't seem to support external name yet. Add here when it does.
    }

    if (isNotEmpty(setupParams.getExternalIPs())) {
      spec.withExternalIPs(
          Arrays.stream(setupParams.getExternalIPs().split(",")).map(String::trim).collect(Collectors.toList()));
    }

    return new ServiceBuilder()
        .withApiVersion("v1")
        .withNewMetadata()
        .withName(serviceName)
        .withNamespace(namespace)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private void cleanup(KubernetesConfig kubernetesConfig, String containerServiceName) {
    int revision = getRevisionFromControllerName(containerServiceName);
    if (revision >= KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision - KEEP_N_REVISIONS + 1;
      ReplicationControllerList replicationControllers =
          kubernetesContainerService.listControllers(kubernetesConfig, emptyList());
      String controllerNamePrefix =
          KubernetesConvention.getReplicationControllerNamePrefixFromControllerName(containerServiceName);
      if (replicationControllers != null) {
        replicationControllers.getItems()
            .stream()
            .filter(c -> c.getMetadata().getName().startsWith(controllerNamePrefix) && c.getSpec().getReplicas() == 0)
            .collect(Collectors.toList())
            .forEach(rc -> {
              String controllerName = rc.getMetadata().getName();
              if (getRevisionFromControllerName(controllerName) < minRevisionToKeep) {
                logger.info("Deleting old version: " + controllerName);
                kubernetesContainerService.deleteController(kubernetesConfig, emptyList(), controllerName);
              }
            });
      }
    }
  }
}
