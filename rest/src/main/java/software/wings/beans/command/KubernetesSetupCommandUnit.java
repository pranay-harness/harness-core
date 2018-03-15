package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.service.impl.KubernetesHelperService.toDisplayYaml;
import static software.wings.service.impl.KubernetesHelperService.toYaml;
import static software.wings.utils.KubernetesConvention.getKubernetesRegistrySecretName;
import static software.wings.utils.KubernetesConvention.getKubernetesServiceName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.fabric8.kubernetes.api.KubernetesHelper;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerSpecBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.extensions.DaemonSet;
import io.fabric8.kubernetes.api.model.extensions.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.DeploymentSpec;
import io.fabric8.kubernetes.api.model.extensions.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSet;
import io.fabric8.kubernetes.api.model.extensions.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetSpec;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.IstioResourceBuilder;
import me.snowdrop.istio.api.model.IstioResourceFluent.RouteRuleSpecNested;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData.ContainerSetupCommandUnitExecutionDataBuilder;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(KubernetesSetupCommandUnit.class);

  @Transient
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";
  public static final String NONE = "none";

  @Transient private static final String SERVICE_NAME_PLACEHOLDER_REGEX = "\\$\\{SERVICE_NAME}";
  @Transient private static final String SERVICE_PORT_PLACEHOLDER_REGEX = "\\$\\{SERVICE_PORT}";

  @Inject @Transient private transient GkeClusterService gkeClusterService;
  @Inject @Transient private transient KubernetesContainerService kubernetesContainerService;
  @Inject @Transient private transient TimeLimiter timeLimiter;
  @Inject @Transient private transient AzureHelperService azureHelperService;

  public KubernetesSetupCommandUnit() {
    super(CommandUnitType.KUBERNETES_SETUP);
    setDeploymentType(DeploymentType.KUBERNETES.name());
  }

  @Override
  protected CommandExecutionStatus executeInternal(CommandExecutionContext context,
      SettingAttribute cloudProviderSetting, List<EncryptedDataDetail> edd, ContainerSetupParams containerSetupParams,
      Map<String, String> serviceVariables, Map<String, String> safeDisplayServiceVariables,
      ExecutionLogCallback executionLogCallback) {
    ContainerSetupCommandUnitExecutionDataBuilder commandExecutionDataBuilder =
        ContainerSetupCommandUnitExecutionData.builder();

    try {
      KubernetesSetupParams setupParams = (KubernetesSetupParams) containerSetupParams;

      KubernetesConfig kubernetesConfig;
      List<EncryptedDataDetail> encryptedDataDetails;
      if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
        kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
        encryptedDataDetails = edd;
      } else if (cloudProviderSetting.getValue() instanceof KubernetesClusterConfig) {
        kubernetesConfig = ((KubernetesClusterConfig) cloudProviderSetting.getValue()).createKubernetesConfig();
        encryptedDataDetails = edd;
      } else if (cloudProviderSetting.getValue() instanceof AzureConfig) {
        AzureConfig azureConfig = (AzureConfig) cloudProviderSetting.getValue();
        kubernetesConfig =
            azureHelperService.getKubernetesClusterConfig(azureConfig, edd, setupParams.getSubscriptionId(),
                setupParams.getResourceGroup(), setupParams.getClusterName(), setupParams.getNamespace());
        kubernetesConfig.setDecrypted(true);
        encryptedDataDetails = emptyList();
      } else {
        kubernetesConfig = gkeClusterService.getCluster(
            cloudProviderSetting, edd, setupParams.getClusterName(), setupParams.getNamespace());
        kubernetesConfig.setDecrypted(true);
        encryptedDataDetails = emptyList();
      }

      String lastCtrlName =
          lastController(kubernetesConfig, encryptedDataDetails, setupParams.getControllerNamePrefix());
      int revision = getRevisionFromControllerName(lastCtrlName).orElse(-1) + 1;

      String kubernetesServiceName = getKubernetesServiceName(setupParams.getControllerNamePrefix());

      if (setupParams.isRollback()) {
        executionLogCallback.saveExecutionLog("Rolling back setup", LogLevel.INFO);
        if (isNotBlank(setupParams.getPreviousDaemonSetYaml())) {
          performDaemonSetRollback(encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig);
        }
        if (isNotEmpty(setupParams.getPreviousActiveAutoscalers())) {
          performAutoscalerRollback(encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig);
        }
        executionLogCallback.saveExecutionLog("Rollback complete", LogLevel.INFO);

        context.setCommandExecutionData(
            commandExecutionDataBuilder.containerServiceName(setupParams.getControllerNamePrefix()).build());

        return CommandExecutionStatus.SUCCESS;
      }

      kubernetesContainerService.createNamespaceIfNotExist(kubernetesConfig, encryptedDataDetails);

      String registrySecretName = getKubernetesRegistrySecretName(setupParams.getImageDetails());
      Secret registrySecret = createRegistrySecret(
          registrySecretName, kubernetesConfig.getNamespace(), setupParams.getImageDetails(), executionLogCallback);
      kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails, registrySecret);

      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) setupParams.getContainerTask();
      if (kubernetesContainerTask == null) {
        kubernetesContainerTask = new KubernetesContainerTask();
        ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
        kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
      }

      boolean isDaemonSet = kubernetesContainerTask.checkDaemonSet();
      String containerServiceName = isDaemonSet
          ? setupParams.getControllerNamePrefix()
          : KubernetesConvention.getControllerName(setupParams.getControllerNamePrefix(), revision);

      String previousDaemonSetYaml =
          isDaemonSet ? getDaemonSetYaml(kubernetesConfig, encryptedDataDetails, containerServiceName) : null;
      List<String> previousActiveAutoscalers = isDaemonSet
          ? null
          : getActiveAutoscalers(kubernetesConfig, encryptedDataDetails, containerServiceName, executionLogCallback);

      commandExecutionDataBuilder.containerServiceName(containerServiceName)
          .previousDaemonSetYaml(previousDaemonSetYaml)
          .previousActiveAutoscalers(previousActiveAutoscalers);

      Map<String, String> serviceLabels =
          ImmutableMap.<String, String>builder()
              .put("app", KubernetesConvention.getLabelValue(setupParams.getAppName()))
              .put("service", KubernetesConvention.getLabelValue(setupParams.getServiceName()))
              .put("env", KubernetesConvention.getLabelValue(setupParams.getEnvName()))
              .build();

      Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                                 .putAll(serviceLabels)
                                                 .put("revision", isDaemonSet ? "ds" : Integer.toString(revision))
                                                 .build();

      // Setup config map
      ConfigMap configMap;
      if (isNotBlank(setupParams.getConfigMapYaml())) {
        try {
          configMap = KubernetesHelper.loadYaml(setupParams.getConfigMapYaml());
          ObjectMeta configMapMeta = Optional.ofNullable(configMap.getMetadata()).orElse(new ObjectMeta());
          configMapMeta.setName(containerServiceName);
          configMapMeta.setNamespace(kubernetesConfig.getNamespace());
          Map<String, String> configMapLabels = new HashMap<>();
          if (isNotEmpty(configMapMeta.getLabels())) {
            configMapLabels.putAll(configMapMeta.getLabels());
          }
          configMapLabels.putAll(controllerLabels);
          configMapMeta.setLabels(configMapLabels);
          configMap.setMetadata(configMapMeta);
        } catch (IOException e) {
          throw new WingsException("Error while loading configMap yaml", e);
        }
      } else {
        configMap = new ConfigMapBuilder()
                        .withNewMetadata()
                        .withName(containerServiceName)
                        .withNamespace(kubernetesConfig.getNamespace())
                        .withLabels(controllerLabels)
                        .endMetadata()
                        .build();

        Map<String, String> data = new HashMap<>();

        if (isNotEmpty(safeDisplayServiceVariables)) {
          data.putAll(safeDisplayServiceVariables.entrySet()
                          .stream()
                          .filter(entry -> !SECRET_MASK.equals(entry.getValue()))
                          .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        if (isNotEmpty(setupParams.getConfigFiles())) {
          data.putAll(setupParams.getConfigFiles().stream().collect(toMap(sa -> sa[0], sa -> sa[1])));
        }
        configMap.setData(data);
      }

      if (configMap != null && isEmpty(configMap.getData())) {
        configMap = null;
      }

      if (configMap != null) {
        executionLogCallback.saveExecutionLog("Creating config map:\n\n" + toDisplayYaml(configMap), LogLevel.INFO);
        kubernetesContainerService.createOrReplaceConfigMap(kubernetesConfig, encryptedDataDetails, configMap);
      }

      // Setup secrets
      Map<String, String> encryptedServiceVars =
          safeDisplayServiceVariables.entrySet()
              .stream()
              .filter(entry -> SECRET_MASK.equals(entry.getValue()))
              .collect(toMap(Entry::getKey, entry -> serviceVariables.get(entry.getKey())));

      Secret secret = null;
      if (isNotEmpty(encryptedServiceVars)) {
        executionLogCallback.saveExecutionLog("Setting secrets: " + containerServiceName + "\n"
            + Joiner.on("\n").join(
                  encryptedServiceVars.keySet().stream().map(var -> "   " + var + ": " + SECRET_MASK).collect(toList()))
            + "\n");
        secret = new SecretBuilder()
                     .withNewMetadata()
                     .withName(containerServiceName)
                     .endMetadata()
                     .withStringData(encryptedServiceVars)
                     .build();
        kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails, secret);
      }

      // Setup controller

      kubernetesContainerService.createController(kubernetesConfig, encryptedDataDetails,
          createKubernetesControllerDefinition(kubernetesContainerTask, containerServiceName, controllerLabels,
              kubernetesConfig.getNamespace(), setupParams.getImageDetails(), registrySecretName, configMap, secret));

      String serviceClusterIP = null;
      String serviceLoadBalancerEndpoint = null;

      // Setup service
      Service service =
          kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);

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
        service = kubernetesContainerService.createOrReplaceService(
            kubernetesConfig, encryptedDataDetails, serviceDefinition);
        serviceClusterIP = service.getSpec().getClusterIP();

        if (setupParams.getServiceType() == KubernetesServiceType.LoadBalancer) {
          serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(
              kubernetesConfig, encryptedDataDetails, service, setupParams.getLoadBalancerIP(), executionLogCallback);
        }

      } else {
        executionLogCallback.saveExecutionLog("Kubernetes service type set to 'None'", LogLevel.INFO);
        if (service != null) {
          executionLogCallback.saveExecutionLog("Deleting existing service " + kubernetesServiceName, LogLevel.INFO);
          kubernetesContainerService.deleteService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
          service = null;
        }
      }

      // Setup ingress
      Ingress ingress = null;
      if (setupParams.isUseIngress() && service != null) {
        ingress = kubernetesContainerService.createOrReplaceIngress(kubernetesConfig, encryptedDataDetails,
            createIngressDefinition(setupParams, service, kubernetesServiceName, executionLogCallback));
      } else {
        try {
          ingress =
              kubernetesContainerService.getIngress(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
          if (ingress != null) {
            kubernetesContainerService.deleteIngress(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
            ingress = null;
          }
        } catch (Exception e) {
          Misc.logAllMessages(e, executionLogCallback);
        }
      }

      // Setup route rule
      IstioResource routeRule = null;
      Map<String, Integer> activeControllers = null;
      int totalInstances = 0;
      if (setupParams.isUseIstioRouteRule()) {
        activeControllers = kubernetesContainerService.getActiveServiceCounts(
            kubernetesConfig, encryptedDataDetails, containerServiceName);
        totalInstances = activeControllers.values().stream().mapToInt(Integer::intValue).sum();
        executionLogCallback.saveExecutionLog(
            String.format("Setting Istio route rule %s - 100%% of traffic %s initially", kubernetesServiceName,
                isNotEmpty(activeControllers) ? "to "
                        + (activeControllers.size() == 1 ? activeControllers.keySet().iterator().next()
                                                         : activeControllers.size() + " previous controllers")
                                              : "disabled"),
            LogLevel.INFO);
        routeRule = kubernetesContainerService.createOrReplaceRouteRule(kubernetesConfig, encryptedDataDetails,
            createRouteRuleDefinition(setupParams, kubernetesServiceName, activeControllers));
      }

      // Disable previous autoscalers
      if (isNotEmpty(previousActiveAutoscalers)) {
        previousActiveAutoscalers.forEach(autoscaler -> {
          executionLogCallback.saveExecutionLog("Disabling autoscaler " + autoscaler, LogLevel.INFO);
          /*
           * Ideally we should be sending apiVersion as "v2beta1" when we are dealing with
           * customMetricHPA, but there is a bug in fabric8 library in HasMetadataOperation.replace() method. For
           * customMetricHPA, metric config info resides in HPA.Spec.additionalProperties map. but during execution of
           * replace(), due to build() method in HorizontalPodAutoscalerSpecBuilder, this map goes away, and replace()
           * call actually removes all metricConfig from autoScalar. So currently use v1 version only, till this issue
           * gets fixed. (customMetricConfig is preserved as annotations in version_v1 HPA object, and that path is
           * working fine)
           * */
          kubernetesContainerService.disableAutoscaler(
              kubernetesConfig, encryptedDataDetails, autoscaler, ContainerApiVersions.KUBERNETES_V1.getVersionName());
        });
      }

      // Create new autoscaler
      if (setupParams.isUseAutoscaler()) {
        HorizontalPodAutoscaler autoscalerDefinition = createAutoscaler(
            containerServiceName, kubernetesConfig.getNamespace(), controllerLabels, setupParams, executionLogCallback);

        if (autoscalerDefinition != null) {
          executionLogCallback.saveExecutionLog(
              String.format("Creating autoscaler %s - disabled until 100%% deployed", containerServiceName),
              LogLevel.INFO);

          kubernetesContainerService.createAutoscaler(kubernetesConfig, encryptedDataDetails, autoscalerDefinition);
        }
      }

      String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();

      executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName(), LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Controller Name: " + containerServiceName, LogLevel.INFO);
      executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName, LogLevel.INFO);
      if (service != null) {
        executionLogCallback.saveExecutionLog("Service Name: " + kubernetesServiceName, LogLevel.INFO);
      }
      if (isNotBlank(serviceClusterIP)) {
        executionLogCallback.saveExecutionLog("Service Cluster IP: " + serviceClusterIP, LogLevel.INFO);
      }
      if (isNotBlank(serviceLoadBalancerEndpoint)) {
        executionLogCallback.saveExecutionLog("Load Balancer Endpoint: " + serviceLoadBalancerEndpoint, LogLevel.INFO);
      }
      if (ingress != null) {
        executionLogCallback.saveExecutionLog("Ingress Name: " + kubernetesServiceName, LogLevel.INFO);
        executionLogCallback.saveExecutionLog("Ingress Rule: " + getIngressRuleString(ingress), LogLevel.INFO);
      }
      if (routeRule != null) {
        executionLogCallback.saveExecutionLog(
            "Istio route rule weights:" + (isEmpty(activeControllers) ? " disabled initially" : ""));
        for (String controller : activeControllers.keySet()) {
          int weight = (int) Math.round((activeControllers.get(controller) * 100.0) / totalInstances);
          executionLogCallback.saveExecutionLog("   " + controller + ": " + weight + "%");
        }
      }

      if (isDaemonSet) {
        listContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
            executionLogCallback, kubernetesConfig, containerServiceName);
      } else {
        executionLogCallback.saveExecutionLog("Cleaning up old versions", LogLevel.INFO);
        cleanup(kubernetesConfig, encryptedDataDetails, containerServiceName, executionLogCallback);
      }

      return CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(commandExecutionDataBuilder.build());
    }
  }

  private String getDaemonSetYaml(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    HasMetadata daemonSet =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, containerServiceName);
    if (daemonSet != null) {
      try {
        return toYaml(daemonSet);
      } catch (IOException e) {
        logger.error("Error converting DaemonSet to yaml: {}", containerServiceName);
      }
    }
    return null;
  }

  private Ingress createIngressDefinition(KubernetesSetupParams setupParams, Service service,
      String kubernetesServiceName, ExecutionLogCallback executionLogCallback) {
    int port = isNotEmpty(service.getSpec().getPorts()) ? service.getSpec().getPorts().get(0).getPort() : 80;
    try {
      Ingress ingress =
          KubernetesHelper.loadYaml(setupParams.getIngressYaml()
                                        .replaceAll(SERVICE_NAME_PLACEHOLDER_REGEX, kubernetesServiceName)
                                        .replaceAll(SERVICE_PORT_PLACEHOLDER_REGEX, Integer.toString(port)));
      ingress.getMetadata().setName(kubernetesServiceName);
      return ingress;
    } catch (IOException e) {
      executionLogCallback.saveExecutionLog(
          "Error reading Ingress from yaml: " + kubernetesServiceName, LogLevel.ERROR);
    }
    return null;
  }

  private List<String> getActiveAutoscalers(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName,
      ExecutionLogCallback executionLogCallback) {
    String controllerNamePrefix = KubernetesConvention.getPrefixFromControllerName(containerServiceName);
    try {
      return kubernetesContainerService.listAutoscalers(kubernetesConfig, encryptedDataDetails)
          .stream()
          .filter(autoscaler -> autoscaler.getMetadata().getName().startsWith(controllerNamePrefix))
          .filter(autoscaler -> !"none".equals(autoscaler.getSpec().getScaleTargetRef().getName()))
          .map(autoscaler -> autoscaler.getMetadata().getName())
          .collect(toList());
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
    }
    return emptyList();
  }

  private HorizontalPodAutoscaler createAutoscaler(String autoscalerName, String namespace,
      Map<String, String> serviceLabels, KubernetesSetupParams setupParams, ExecutionLogCallback executionLogCallback) {
    HorizontalPodAutoscaler horizontalPodAutoscaler;

    if (isNotEmpty(setupParams.getCustomMetricYamlConfig())) {
      executionLogCallback.saveExecutionLog(
          String.format("Setting autoscaler with custom metric config: %s", setupParams.getCustomMetricYamlConfig()),
          LogLevel.INFO);
      horizontalPodAutoscaler =
          getCustomMetricHorizontalPodAutoscalar(autoscalerName, namespace, serviceLabels, setupParams);
    } else {
      executionLogCallback.saveExecutionLog(
          String.format("Setting autoscaler min instances %d, max instances %d, with target CPU utilization %d%%",
              setupParams.getMinAutoscaleInstances(), setupParams.getMaxAutoscaleInstances(),
              setupParams.getTargetCpuUtilizationPercentage()),
          LogLevel.INFO);

      horizontalPodAutoscaler = getBasicHorizontalPodAutoscaler(autoscalerName, namespace, serviceLabels, setupParams);
    }

    return horizontalPodAutoscaler;
  }

  private HorizontalPodAutoscaler getCustomMetricHorizontalPodAutoscalar(
      String autoscalerName, String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    try {
      HorizontalPodAutoscaler horizontalPodAutoscaler =
          KubernetesHelper.loadYaml(setupParams.getCustomMetricYamlConfig());

      // set kind/name to none
      horizontalPodAutoscaler.getSpec().getScaleTargetRef().setName(NONE);
      horizontalPodAutoscaler.getSpec().getScaleTargetRef().setKind(NONE);

      // create metadata
      ObjectMeta objectMeta = horizontalPodAutoscaler.getMetadata();
      if (objectMeta == null) {
        objectMeta = new ObjectMeta();
        horizontalPodAutoscaler.setMetadata(objectMeta);
      }

      // set labels, name and namespace
      objectMeta.setLabels(serviceLabels);
      objectMeta.setName(autoscalerName);
      objectMeta.setNamespace(namespace);

      return horizontalPodAutoscaler;
    } catch (IOException e) {
      throw new WingsException("Error while loading customMetricYaml for horizontal pod autoscaling", e);
    }
  }

  private HorizontalPodAutoscaler getBasicHorizontalPodAutoscaler(
      String autoscalerName, String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    HorizontalPodAutoscalerSpecBuilder spec =
        new HorizontalPodAutoscalerSpecBuilder()
            .withMinReplicas(setupParams.getMinAutoscaleInstances())
            .withMaxReplicas(setupParams.getMaxAutoscaleInstances())
            .withTargetCPUUtilizationPercentage(setupParams.getTargetCpuUtilizationPercentage())
            .withNewScaleTargetRef()
            .withKind(NONE)
            .withName(NONE)
            .endScaleTargetRef();
    return new HorizontalPodAutoscalerBuilder()
        .withNewMetadata()
        .withName(autoscalerName)
        .withNamespace(namespace)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private IstioResource createRouteRuleDefinition(
      KubernetesSetupParams setupParams, String kubernetesServiceName, Map<String, Integer> activeControllers) {
    RouteRuleSpecNested<IstioResourceBuilder> routeRuleSpecNested = new IstioResourceBuilder()
                                                                        .withNewMetadata()
                                                                        .withName(kubernetesServiceName)
                                                                        .withNamespace(setupParams.getNamespace())
                                                                        .endMetadata()
                                                                        .withNewRouteRuleSpec()
                                                                        .withNewDestination()
                                                                        .withName(kubernetesServiceName)
                                                                        .withNamespace(setupParams.getNamespace())
                                                                        .endDestination();
    int totalInstances = activeControllers.values().stream().mapToInt(Integer::intValue).sum();
    for (String controller : activeControllers.keySet()) {
      int revision = getRevisionFromControllerName(controller).orElse(-1);
      routeRuleSpecNested.addNewRoute()
          .addToLabels("revision", Integer.toString(revision))
          .withWeight((int) Math.round((activeControllers.get(controller) * 100.0) / totalInstances))
          .endRoute();
    }

    return routeRuleSpecNested.endRouteRuleSpec().build();
  }

  private void listContainerInfosWhenReady(List<EncryptedDataDetail> encryptedDataDetails,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback, KubernetesConfig kubernetesConfig,
      String containerServiceName) {
    int desiredCount = kubernetesContainerService.getNodes(kubernetesConfig, encryptedDataDetails).getItems().size();
    int previousCount =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, containerServiceName) != null
        ? desiredCount
        : 0;
    List<ContainerInfo> containerInfos =
        kubernetesContainerService.getContainerInfosWhenReady(kubernetesConfig, encryptedDataDetails,
            containerServiceName, previousCount, desiredCount, serviceSteadyStateTimeout, executionLogCallback);

    boolean allContainersSuccess =
        containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
    if (containerInfos.size() != desiredCount || !allContainersSuccess) {
      if (containerInfos.size() != desiredCount) {
        String message = String.format("Expected data for %d container%s but got %d", desiredCount,
            containerInfos.size() == 1 ? "" : "s", containerInfos.size());
        executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
      }
      if (!allContainersSuccess) {
        List<ContainerInfo> failed =
            containerInfos.stream().filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS).collect(toList());
        String message = String.format("The following container%s did not have success status: %s",
            failed.size() == 1 ? "" : "s", failed.stream().map(ContainerInfo::getContainerId).collect(toList()));
        executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
      }
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE)
          .addParam("message", "DaemonSet pods failed to reach desired count");
    }
    containerInfos.forEach(info
        -> executionLogCallback.saveExecutionLog("DaemonSet container ID: " + info.getContainerId(), LogLevel.INFO));
  }

  private void performDaemonSetRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig) {
    String daemonSetName = setupParams.getControllerNamePrefix();
    String daemonSetYaml = setupParams.getPreviousDaemonSetYaml();
    if (isNotBlank(daemonSetYaml)) {
      try {
        DaemonSet daemonSet = KubernetesHelper.loadYaml(daemonSetYaml);
        executionLogCallback.saveExecutionLog("Rolling back DaemonSet " + daemonSetName, LogLevel.INFO);
        kubernetesContainerService.createController(kubernetesConfig, encryptedDataDetails, daemonSet);
        executionLogCallback.saveExecutionLog("Rolled back to DaemonSet with image: "
                + daemonSet.getSpec()
                      .getTemplate()
                      .getSpec()
                      .getContainers()
                      .stream()
                      .map(Container::getImage)
                      .collect(toList()),
            LogLevel.INFO);
        listContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
            executionLogCallback, kubernetesConfig, daemonSetName);
      } catch (IOException e) {
        executionLogCallback.saveExecutionLog("Error reading DaemonSet from yaml: " + daemonSetName, LogLevel.ERROR);
      }
    } else {
      executionLogCallback.saveExecutionLog(
          "DaemonSet " + daemonSetName + " did not exist previously. Deleting on rollback", LogLevel.INFO);
      HasMetadata daemonSet =
          kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, daemonSetName);
      Map<String, String> labels = daemonSet.getMetadata().getLabels();
      kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, daemonSetName);
      kubernetesContainerService.waitForPodsToStop(kubernetesConfig, encryptedDataDetails, labels,
          setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
    }
  }

  private void performAutoscalerRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig) {
    List<String> autoscalerNames = setupParams.getPreviousActiveAutoscalers();
    if (isNotEmpty(autoscalerNames)) {
      for (String autoscalerName : autoscalerNames) {
        executionLogCallback.saveExecutionLog("Enabling autoscaler " + autoscalerName, LogLevel.INFO);
        /*
         * Ideally we should be sending apiVersion as "v2beta1" when we are dealing with
         * customMetricHPA, but there is a bug in fabric8 library in HasMetadataOperation.replace() method. For
         * customMetricHPA, metric config info resides in HPA.Spec.additionalProperties map. but during execution of
         * replace(), due to build() method in HorizontalPodAutoscalerSpecBuilder, this map goes away, and replace()
         * call actually removes all metricConfig from autoScalar. So currently use v1 version only, till this issue
         * gets fixed. (customMetricConfig is preserved as annotations in version_v1 HPA object, and that path is
         * working fine)
         * */
        kubernetesContainerService.enableAutoscaler(kubernetesConfig, encryptedDataDetails, autoscalerName,
            ContainerApiVersions.KUBERNETES_V1.getVersionName());
      }
    }
  }

  public String getApiVersionForHPA(String yamlConfig) {
    return (StringUtils.isEmpty(yamlConfig) || StringUtils.isBlank(yamlConfig))
        ? ContainerApiVersions.KUBERNETES_V1.getVersionName()
        : ContainerApiVersions.KUBERNETES_V2_BETA1.getVersionName();
  }

  private Secret createRegistrySecret(
      String secretName, String namespace, ImageDetails imageDetails, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Setting image pull secret " + secretName, LogLevel.INFO);
    String credentialData = String.format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
        imageDetails.getUsername(), imageDetails.getPassword());
    Map<String, String> data =
        ImmutableMap.of(".dockercfg", new String(Base64.getEncoder().encode(credentialData.getBytes())));
    return new SecretBuilder()
        .withNewMetadata()
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType("kubernetes.io/dockercfg")
        .withData(data)
        .build();
  }

  private String waitForLoadBalancerEndpoint(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, Service service, String loadBalancerIP,
      ExecutionLogCallback executionLogCallback) {
    String serviceName = service.getMetadata().getName();
    LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
    if (loadBalancer != null
        && (loadBalancer.getIngress().isEmpty()
               || (isNotEmpty(loadBalancerIP) && !loadBalancerIP.equals(loadBalancer.getIngress().get(0).getIp())))) {
      executionLogCallback.saveExecutionLog(
          "Waiting for service " + serviceName + " load balancer to be ready", LogLevel.INFO);
      try {
        return timeLimiter.callWithTimeout(() -> {
          while (true) {
            LoadBalancerStatus loadBalancerStatus =
                kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, serviceName)
                    .getStatus()
                    .getLoadBalancer();
            if (!loadBalancerStatus.getIngress().isEmpty()
                && (isEmpty(loadBalancerIP) || loadBalancerIP.equals(loadBalancerStatus.getIngress().get(0).getIp()))) {
              return getLoadBalancerEndpoint(executionLogCallback, serviceName, loadBalancerStatus);
            }
            sleep(ofSeconds(1));
          }
        }, 5L, TimeUnit.MINUTES, true);
      } catch (UncheckedTimeoutException e) {
        executionLogCallback.saveExecutionLog(
            String.format("Timed out waiting for service [%s] load balancer to be ready", serviceName), LogLevel.ERROR);
      } catch (Exception e) {
        Misc.logAllMessages(e, executionLogCallback);
      }
    } else if (loadBalancer != null && !loadBalancer.getIngress().isEmpty()) {
      return getLoadBalancerEndpoint(executionLogCallback, serviceName, loadBalancer);
    }
    return null;
  }

  private String getLoadBalancerEndpoint(
      ExecutionLogCallback executionLogCallback, String serviceName, LoadBalancerStatus loadBalancer) {
    LoadBalancerIngress loadBalancerIngress = loadBalancer.getIngress().get(0);
    String loadBalancerEndpoint =
        isNotEmpty(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname() : loadBalancerIngress.getIp();
    executionLogCallback.saveExecutionLog(
        String.format("Service [%s] load balancer is ready with endpoint [%s]", serviceName, loadBalancerEndpoint),
        LogLevel.INFO);
    return loadBalancerEndpoint;
  }

  private String lastController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String controllerNamePrefix) {
    final HasMetadata[] lastReplicationController = {null};
    final AtomicInteger lastRevision = new AtomicInteger();
    kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix + KubernetesConvention.DOT))
        .forEach(ctrl -> {
          Optional<Integer> revision = getRevisionFromControllerName(ctrl.getMetadata().getName());
          if (revision.isPresent() && (lastReplicationController[0] == null || revision.get() > lastRevision.get())) {
            lastReplicationController[0] = ctrl;
            lastRevision.set(revision.get());
          }
        });
    return lastReplicationController[0] != null ? lastReplicationController[0].getMetadata().getName() : null;
  }

  private HasMetadata createKubernetesControllerDefinition(KubernetesContainerTask kubernetesContainerTask,
      String replicationControllerName, Map<String, String> controllerLabels, String namespace,
      ImageDetails imageDetails, String registrySecretName, ConfigMap configMap, Secret secret) {
    String containerName = KubernetesConvention.getContainerName(imageDetails.getName());
    String imageNameTag = imageDetails.getName() + ":" + imageDetails.getTag();

    HasMetadata kubernetesObj =
        kubernetesContainerTask.createController(containerName, imageNameTag, registrySecretName);

    KubernetesHelper.setName(kubernetesObj, replicationControllerName);
    KubernetesHelper.setNamespace(kubernetesObj, namespace);
    KubernetesHelper.getOrCreateLabels(kubernetesObj).putAll(controllerLabels);

    configureTypeSpecificSpecs(controllerLabels, kubernetesObj, configMap, secret);

    return kubernetesObj;
  }

  private void configureTypeSpecificSpecs(
      Map<String, String> controllerLabels, HasMetadata kubernetesObj, ConfigMap configMap, Secret secret) {
    ObjectMeta objectMeta = null;
    PodSpec podSpec = null;
    if (kubernetesObj instanceof ReplicationController) {
      ReplicationControllerSpec rcSpec = ((ReplicationController) kubernetesObj).getSpec();
      rcSpec.setSelector(controllerLabels);
      rcSpec.setReplicas(0);
      objectMeta = rcSpec.getTemplate().getMetadata();
      podSpec = rcSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof Deployment) {
      DeploymentSpec depSpec = ((Deployment) kubernetesObj).getSpec();
      depSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      depSpec.setReplicas(0);
      objectMeta = depSpec.getTemplate().getMetadata();
      podSpec = depSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof DaemonSet) {
      DaemonSetSpec dsSpec = ((DaemonSet) kubernetesObj).getSpec();
      dsSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      objectMeta = dsSpec.getTemplate().getMetadata();
      podSpec = dsSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof ReplicaSet) {
      ReplicaSetSpec repSetSpec = ((ReplicaSet) kubernetesObj).getSpec();
      repSetSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      repSetSpec.setReplicas(0);
      objectMeta = repSetSpec.getTemplate().getMetadata();
      podSpec = repSetSpec.getTemplate().getSpec();
    } else if (kubernetesObj instanceof StatefulSet) {
      StatefulSetSpec stateSetSpec = ((StatefulSet) kubernetesObj).getSpec();
      stateSetSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      stateSetSpec.setReplicas(0);
      objectMeta = stateSetSpec.getTemplate().getMetadata();
      podSpec = stateSetSpec.getTemplate().getSpec();
    }
    if (objectMeta != null) {
      Map<String, String> labels = objectMeta.getLabels();
      if (labels == null) {
        labels = new HashMap<>();
        objectMeta.setLabels(labels);
      }
      objectMeta.getLabels().putAll(controllerLabels);
    }

    if (podSpec != null) {
      Map<String, EnvVar> secretEnvVars = secret == null
          ? null
          : secret.getStringData().entrySet().stream().collect(toMap(Map.Entry::getKey,
                entry
                -> new EnvVarBuilder()
                       .withName(entry.getKey())
                       .withValueFrom(new EnvVarSourceBuilder()
                                          .withNewSecretKeyRef()
                                          .withName(secret.getMetadata().getName())
                                          .withKey(entry.getKey())
                                          .endSecretKeyRef()
                                          .build())
                       .build()));
      for (Container container : podSpec.getContainers()) {
        if (configMap != null) {
          List<EnvFromSource> envSourceList = new ArrayList<>();
          if (container.getEnvFrom() != null) {
            envSourceList.addAll(container.getEnvFrom());
          }
          envSourceList.add(new EnvFromSourceBuilder()
                                .withNewConfigMapRef()
                                .withName(configMap.getMetadata().getName())
                                .endConfigMapRef()
                                .build());
          container.setEnvFrom(envSourceList);
        }
        if (secretEnvVars != null) {
          Map<String, EnvVar> containerEnvVars = new HashMap<>();
          if (container.getEnv() != null) {
            container.getEnv().forEach(envVar -> containerEnvVars.put(envVar.getName(), envVar));
          }
          containerEnvVars.putAll(secretEnvVars);
          container.setEnv(new ArrayList<>(containerEnvVars.values()));
        }
      }
    }
  }

  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(
      String serviceName, String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    ServiceSpecBuilder spec =
        new ServiceSpecBuilder().addToSelector(serviceLabels).withType(setupParams.getServiceType().name());

    if (setupParams.getServiceType() != KubernetesServiceType.ExternalName) {
      ServicePortBuilder servicePort =
          new ServicePortBuilder()
              .withProtocol(setupParams.getProtocol().name())
              .withPort(setupParams.getPort())
              .withNewTargetPort()
              .withIntVal(setupParams.getTargetPort())
              .endTargetPort()
              .withName(isNotBlank(setupParams.getPortName()) ? setupParams.getPortName() : "http");
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
      spec.withExternalName(setupParams.getExternalName());
    }

    if (isNotEmpty(setupParams.getExternalIPs())) {
      spec.withExternalIPs(Arrays.stream(setupParams.getExternalIPs().split(",")).map(String::trim).collect(toList()));
    }

    return new ServiceBuilder()
        .withNewMetadata()
        .withName(serviceName)
        .withNamespace(namespace)
        .addToLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private void cleanup(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String containerServiceName, ExecutionLogCallback executionLogCallback) {
    Optional<Integer> revision = getRevisionFromControllerName(containerServiceName);
    if (revision.isPresent() && revision.get() >= KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision.get() - KEEP_N_REVISIONS + 1;
      String controllerNamePrefix = KubernetesConvention.getPrefixFromControllerName(containerServiceName);
      kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
          .stream()
          .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
          .filter(ctrl -> kubernetesContainerService.getControllerPodCount(ctrl) == 0)
          .forEach(ctrl -> {
            String controllerName = ctrl.getMetadata().getName();
            Optional<Integer> ctrlRevision = getRevisionFromControllerName(controllerName);
            if (ctrlRevision.isPresent() && ctrlRevision.get() < minRevisionToKeep) {
              logger.info("Deleting old version: " + controllerName);
              try {
                kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, controllerName);
                kubernetesContainerService.deleteAutoscaler(kubernetesConfig, encryptedDataDetails, controllerName);
                kubernetesContainerService.deleteConfigMap(kubernetesConfig, encryptedDataDetails, controllerName);
                kubernetesContainerService.deleteSecret(kubernetesConfig, encryptedDataDetails, controllerName);
              } catch (Exception e) {
                Misc.logAllMessages(e, executionLogCallback);
              }
            }
          });
    }
  }

  private String getIngressRuleString(Ingress ingress) {
    String path;
    String port;
    String serviceName;
    String host;
    try {
      IngressRule ingressRule = ingress.getSpec().getRules().get(0);
      HTTPIngressPath httpIngressPath = ingressRule.getHttp().getPaths().get(0);
      path = httpIngressPath.getPath();
      port = httpIngressPath.getBackend().getServicePort().getIntVal().toString();
      serviceName = httpIngressPath.getBackend().getServiceName();
      host = ingressRule.getHost();
    } catch (Exception e) {
      logger.error("Couldn't get path from ingress rule.", e);
      return "ERROR - " + Misc.getMessage(e);
    }
    return (isNotBlank(host) ? host : "") + ":" + port + path + " -> " + serviceName;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("KUBERNETES_SETUP")
  public static class Yaml extends ContainerSetupCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.KUBERNETES_SETUP.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.KUBERNETES_SETUP.name(), deploymentType);
    }
  }
}
