package software.wings.beans.command;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.beans.command.ContainerResizeCommandUnit.DASH_STRING;
import static software.wings.beans.container.KubernetesContainerTask.CONFIG_MAP_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.KubernetesContainerTask.SECRET_MAP_NAME_PLACEHOLDER_REGEX;
import static software.wings.common.Constants.HARNESS_APP;
import static software.wings.common.Constants.HARNESS_ENV;
import static software.wings.common.Constants.HARNESS_REVISION;
import static software.wings.common.Constants.HARNESS_SERVICE;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.service.impl.KubernetesHelperService.printRouteRuleWeights;
import static software.wings.service.impl.KubernetesHelperService.toDisplayYaml;
import static software.wings.service.impl.KubernetesHelperService.toYaml;
import static software.wings.utils.KubernetesConvention.DASH;
import static software.wings.utils.KubernetesConvention.DOT;
import static software.wings.utils.KubernetesConvention.getKubernetesRegistrySecretName;
import static software.wings.utils.KubernetesConvention.getKubernetesServiceName;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;
import static software.wings.utils.message.MessageConstants.DELEGATE_DATA;
import static software.wings.utils.message.MessageConstants.DELEGATE_NAME;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
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
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerBuilder;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerSpecBuilder;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.LoadBalancerIngress;
import io.fabric8.kubernetes.api.model.LoadBalancerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
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
import me.snowdrop.istio.api.model.v1.routing.RouteRule;
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
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Misc;
import software.wings.utils.message.MessageService;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Created by brett on 3/3/17
 */
public class KubernetesSetupCommandUnit extends ContainerSetupCommandUnit {
  @Transient private static final Logger logger = LoggerFactory.getLogger(KubernetesSetupCommandUnit.class);

  @Transient private static final Pattern envVarPattern = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  @Transient
  private static final String DOCKER_REGISTRY_CREDENTIAL_TEMPLATE =
      "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";
  @Transient private static final String NONE = "none";
  @Transient private static final String LOAD_BALANCER = "LoadBalancer";
  @Transient private static final String NODE_PORT = "NodePort";

  @Transient private static final String SERVICE_NAME_PLACEHOLDER_REGEX = "\\$\\{SERVICE_NAME}";
  @Transient private static final String SERVICE_PORT_PLACEHOLDER_REGEX = "\\$\\{SERVICE_PORT}";

  @Inject private transient GkeClusterService gkeClusterService;
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient TimeLimiter timeLimiter;
  @Inject private transient Clock clock;
  @Inject private transient AzureHelperService azureHelperService;
  @Inject private transient MessageService messageService;

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
        KubernetesClusterConfig config = (KubernetesClusterConfig) cloudProviderSetting.getValue();
        String delegateName = messageService.getData(DELEGATE_DATA, DELEGATE_NAME, String.class);
        if (config.isUseKubernetesDelegate() && !Objects.equals(delegateName, config.getDelegateName())) {
          throw new InvalidRequestException(String.format("Kubernetes delegate name [%s] doesn't match "
                  + "cloud provider delegate name [%s] for kubernetes cluster cloud provider [%s]",
              delegateName, config.getDelegateName(), cloudProviderSetting.getName()));
        }
        kubernetesConfig = config.createKubernetesConfig(setupParams.getNamespace());

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

      kubernetesContainerService.createNamespaceIfNotExist(kubernetesConfig, encryptedDataDetails);

      String kubernetesServiceName = getKubernetesServiceName(setupParams.getControllerNamePrefix());

      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) setupParams.getContainerTask();
      if (kubernetesContainerTask == null) {
        kubernetesContainerTask = new KubernetesContainerTask();
        ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
        kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
      }

      boolean isStatefulSet = kubernetesContainerTask.checkStatefulSet();
      boolean isDaemonSet = kubernetesContainerTask.checkDaemonSet();

      String lastCtrlName =
          lastController(kubernetesConfig, encryptedDataDetails, setupParams.getControllerNamePrefix(), isStatefulSet);
      int revision = getRevisionFromControllerName(lastCtrlName, isStatefulSet).orElse(-1) + 1;

      String containerServiceName = isDaemonSet
          ? setupParams.getControllerNamePrefix()
          : KubernetesConvention.getControllerName(setupParams.getControllerNamePrefix(), revision, isStatefulSet);

      if (setupParams.isRollback()) {
        executionLogCallback.saveExecutionLog("Rolling back setup");
        if (isDaemonSet) {
          performDaemonSetRollback(encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig);
        }
        if (isNotEmpty(setupParams.getPreviousActiveAutoscalers())) {
          performAutoscalerRollback(encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig);
        }
        executionLogCallback.saveExecutionLog("Rollback complete");
        executionLogCallback.saveExecutionLog(DASH_STRING + "\n");

        context.setCommandExecutionData(
            commandExecutionDataBuilder.containerServiceName(setupParams.getControllerNamePrefix()).build());

        return CommandExecutionStatus.SUCCESS;
      }

      String registrySecretName = getKubernetesRegistrySecretName(setupParams.getImageDetails());
      Secret registrySecret = createRegistrySecret(
          registrySecretName, setupParams.getNamespace(), setupParams.getImageDetails(), executionLogCallback);
      kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails, registrySecret);

      String previousDaemonSetYaml = null;
      List<Pod> originalDaemonSetPods = null;
      List<String> previousActiveAutoscalers = null;
      Map<String, Integer> activeServiceCounts = new HashMap<>();
      Map<String, Integer> trafficWeights = new HashMap<>();

      if (isDaemonSet) {
        previousDaemonSetYaml = getDaemonSetYaml(kubernetesConfig, encryptedDataDetails, containerServiceName);
        originalDaemonSetPods =
            kubernetesContainerService.getRunningPods(kubernetesConfig, encryptedDataDetails, containerServiceName);
      } else {
        previousActiveAutoscalers = getActiveAutoscalers(
            kubernetesConfig, encryptedDataDetails, containerServiceName, executionLogCallback, isStatefulSet);
        activeServiceCounts = kubernetesContainerService.getActiveServiceCounts(
            kubernetesConfig, encryptedDataDetails, containerServiceName, isStatefulSet);
        trafficWeights = kubernetesContainerService.getTrafficWeights(
            kubernetesConfig, encryptedDataDetails, containerServiceName, isStatefulSet);
      }

      commandExecutionDataBuilder.containerServiceName(containerServiceName)
          .previousDaemonSetYaml(previousDaemonSetYaml)
          .previousActiveAutoscalers(previousActiveAutoscalers)
          .activeServiceCounts(integerMapToListOfStringArray(activeServiceCounts))
          .trafficWeights(integerMapToListOfStringArray(trafficWeights));

      Map<String, String> serviceLabels =
          ImmutableMap.<String, String>builder()
              .put(HARNESS_APP, KubernetesConvention.getLabelValue(setupParams.getAppName()))
              .put(HARNESS_SERVICE, KubernetesConvention.getLabelValue(setupParams.getServiceName()))
              .put(HARNESS_ENV, KubernetesConvention.getLabelValue(setupParams.getEnvName()))
              .build();

      Map<String, String> controllerLabels = ImmutableMap.<String, String>builder()
                                                 .putAll(serviceLabels)
                                                 .put(HARNESS_REVISION, isDaemonSet ? "ds" : Integer.toString(revision))
                                                 .build();

      // Setup config map
      ConfigMap configMap = prepareConfigMap(kubernetesConfig, encryptedDataDetails, setupParams, containerServiceName,
          controllerLabels, safeDisplayServiceVariables, executionLogCallback);

      // Setup secret map
      Secret secretMap = prepareSecretMap(kubernetesConfig, encryptedDataDetails, setupParams, containerServiceName,
          controllerLabels, safeDisplayServiceVariables, serviceVariables, executionLogCallback);

      long daemonSetStartTime = clock.millis();

      // Setup controller
      kubernetesContainerService.createController(kubernetesConfig, encryptedDataDetails,
          createKubernetesControllerDefinition(kubernetesContainerTask, containerServiceName, controllerLabels,
              setupParams.getNamespace(), setupParams.getImageDetails(), registrySecretName, configMap, secretMap,
              executionLogCallback));

      String serviceClusterIP = null;
      String serviceLoadBalancerEndpoint = null;
      String nodePort = null;

      // Setup service
      Service service = null;

      try {
        service = kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
      } catch (Exception e) {
        Misc.logAllMessages(e, executionLogCallback);
      }

      if (setupParams.getServiceType() != null && setupParams.getServiceType() != KubernetesServiceType.None) {
        Service serviceDefinition = setupParams.getServiceType() == KubernetesServiceType.Yaml
            ? createdServiceDefinitionFromYaml(
                  kubernetesServiceName, setupParams.getNamespace(), serviceLabels, setupParams, executionLogCallback)
            : createServiceDefinition(
                  kubernetesServiceName, setupParams.getNamespace(), serviceLabels, setupParams, executionLogCallback);
        if (service != null) {
          // Keep the previous load balancer IP if it exists and a new one was not specified
          LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
          if (serviceDefinition.getSpec().getType().equals(LOAD_BALANCER)
              && isEmpty(serviceDefinition.getSpec().getLoadBalancerIP()) && loadBalancer != null
              && !loadBalancer.getIngress().isEmpty()) {
            serviceDefinition.getSpec().setLoadBalancerIP(loadBalancer.getIngress().get(0).getIp());
          }
        }
        service = kubernetesContainerService.createOrReplaceService(
            kubernetesConfig, encryptedDataDetails, serviceDefinition);
        serviceClusterIP = service.getSpec().getClusterIP();

        if (service.getSpec().getType().equals(LOAD_BALANCER)) {
          serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(
              kubernetesConfig, encryptedDataDetails, service, setupParams.getLoadBalancerIP(), executionLogCallback);
        } else if (service.getSpec().getType().equals(NODE_PORT)) {
          nodePort = Joiner.on(',').join(
              service.getSpec().getPorts().stream().map(ServicePort::getNodePort).collect(toList()));
        }

      } else {
        executionLogCallback.saveExecutionLog("Kubernetes service type set to 'None'");
        if (service != null) {
          try {
            if (service.getSpec().getSelector().containsKey(HARNESS_APP)) {
              executionLogCallback.saveExecutionLog("Deleting existing service " + kubernetesServiceName);
              kubernetesContainerService.deleteService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
            }
          } catch (Exception e) {
            logger.error("Couldn't delete service {}", e);
          }
          service = null;
        }
      }

      // Setup ingress
      Ingress ingress = prepareIngress(kubernetesConfig, encryptedDataDetails, setupParams, kubernetesServiceName,
          containerServiceName, service, executionLogCallback);

      // Setup Istio route rule
      IstioResource routeRule = prepareRouteRule(encryptedDataDetails, kubernetesConfig, setupParams,
          kubernetesServiceName, service, activeServiceCounts, executionLogCallback, isStatefulSet);

      // Disable previous autoscalers
      if (isNotEmpty(previousActiveAutoscalers)) {
        previousActiveAutoscalers.forEach(
            autoscaler -> disableAutoscaler(kubernetesConfig, encryptedDataDetails, autoscaler, executionLogCallback));
      }

      // Create new autoscaler
      HorizontalPodAutoscaler hpa = prepareHorizontalPodAutoscaler(kubernetesConfig, setupParams, encryptedDataDetails,
          containerServiceName, controllerLabels, executionLogCallback);

      // Print execution summary
      String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();

      executionLogCallback.saveExecutionLog("Cluster Name: " + setupParams.getClusterName());
      executionLogCallback.saveExecutionLog("Controller Name: " + containerServiceName);
      executionLogCallback.saveExecutionLog("Docker Image Name: " + dockerImageName);
      if (configMap != null) {
        executionLogCallback.saveExecutionLog("Config Map: " + configMap.getMetadata().getName());
      }
      if (secretMap != null) {
        executionLogCallback.saveExecutionLog("Secret Map: " + secretMap.getMetadata().getName());
      }
      if (hpa != null) {
        executionLogCallback.saveExecutionLog("Horizontal Pod Autoscaler: " + hpa.getMetadata().getName());
      }
      if (service != null) {
        executionLogCallback.saveExecutionLog("Service Name: " + kubernetesServiceName);
      }
      if (isNotBlank(serviceClusterIP)) {
        executionLogCallback.saveExecutionLog("Service Cluster IP: " + serviceClusterIP);
      }
      if (isNotBlank(serviceLoadBalancerEndpoint)) {
        executionLogCallback.saveExecutionLog("Load Balancer Endpoint: " + serviceLoadBalancerEndpoint);
      }
      if (isNotBlank(nodePort)) {
        executionLogCallback.saveExecutionLog("Node Port: " + nodePort);
      }
      if (ingress != null) {
        executionLogCallback.saveExecutionLog("Ingress Name: " + ingress.getMetadata().getName());
        printIngressRules(ingress, executionLogCallback);
      }
      if (routeRule != null) {
        executionLogCallback.saveExecutionLog("Istio route rule: " + routeRule.getMetadata().getName());
        printRouteRuleWeights(
            routeRule, getPrefixFromControllerName(containerServiceName, isStatefulSet), executionLogCallback);
      }

      if (isDaemonSet) {
        listDaemonSetContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
            executionLogCallback, kubernetesConfig, containerServiceName, originalDaemonSetPods, daemonSetStartTime,
            false);
      } else {
        executionLogCallback.saveExecutionLog("Cleaning up old versions");
        cleanup(kubernetesConfig, encryptedDataDetails, containerServiceName, executionLogCallback, isStatefulSet);
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

  private HorizontalPodAutoscaler prepareHorizontalPodAutoscaler(KubernetesConfig kubernetesConfig,
      KubernetesSetupParams setupParams, List<EncryptedDataDetail> encryptedDataDetails, String hpaName,
      Map<String, String> controllerLabels, ExecutionLogCallback executionLogCallback) {
    HorizontalPodAutoscaler hpa = null;
    if (setupParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler autoscalerDefinition =
          createAutoscaler(hpaName, setupParams.getNamespace(), controllerLabels, setupParams, executionLogCallback);

      if (autoscalerDefinition != null) {
        executionLogCallback.saveExecutionLog(
            format("Autoscaler %s - disabled until 100%% deployed", hpaName), LogLevel.INFO);

        hpa = kubernetesContainerService.createAutoscaler(kubernetesConfig, encryptedDataDetails, autoscalerDefinition);
      }
    }
    return hpa;
  }

  private IstioResource prepareRouteRule(List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesConfig kubernetesConfig, KubernetesSetupParams setupParams, String routeRuleName, Service service,
      Map<String, Integer> activeControllers, ExecutionLogCallback executionLogCallback, boolean isStatefulSet) {
    IstioResource routeRule = null;
    try {
      routeRule = kubernetesContainerService.getRouteRule(kubernetesConfig, encryptedDataDetails, routeRuleName);
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
    }

    if (setupParams.isUseIstioRouteRule() && service != null) {
      if (routeRule == null || routeRule.getSpec() == null || isEmpty(((RouteRule) routeRule.getSpec()).getRoute())
          || !((RouteRule) routeRule.getSpec()).getRoute().get(0).getLabels().containsKey(HARNESS_REVISION)) {
        IstioResource routeRuleDefinition = createRouteRuleDefinition(setupParams, routeRuleName,
            service.getSpec().getSelector(), activeControllers, isStatefulSet, executionLogCallback);
        routeRule = kubernetesContainerService.createOrReplaceRouteRule(
            kubernetesConfig, encryptedDataDetails, routeRuleDefinition);
      } else {
        executionLogCallback.saveExecutionLog("Istio route rule exists:\n\n" + toDisplayYaml(routeRule));
      }
    } else if (routeRule != null) {
      try {
        if (routeRule.getMetadata().getLabels().containsKey(HARNESS_APP)) {
          executionLogCallback.saveExecutionLog("Deleting Istio route rule " + routeRuleName);
          kubernetesContainerService.deleteRouteRule(kubernetesConfig, encryptedDataDetails, routeRuleName);
        }
      } catch (Exception e) {
        Misc.logAllMessages(e, executionLogCallback);
      }
      routeRule = null;
    }
    return routeRule;
  }

  private Ingress prepareIngress(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesSetupParams setupParams, String ingressName, String containerServiceName, Service service,
      ExecutionLogCallback executionLogCallback) {
    Ingress ingress;
    if (setupParams.isUseIngress() && service != null) {
      ingress = kubernetesContainerService.createOrReplaceIngress(kubernetesConfig, encryptedDataDetails,
          createIngressDefinition(setupParams, service, ingressName, containerServiceName, executionLogCallback));
    } else {
      try {
        ingress = kubernetesContainerService.getIngress(kubernetesConfig, encryptedDataDetails, ingressName);
        if (ingress != null && ingress.getMetadata().getLabels().containsKey(HARNESS_APP)) {
          kubernetesContainerService.deleteIngress(kubernetesConfig, encryptedDataDetails, ingressName);
        }
      } catch (Exception e) {
        Misc.logAllMessages(e, executionLogCallback);
      }
      ingress = null;
    }
    return ingress;
  }

  private Secret prepareSecretMap(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesSetupParams setupParams, String secretMapName, Map<String, String> controllerLabels,
      Map<String, String> safeDisplayServiceVariables, Map<String, String> serviceVariables,
      ExecutionLogCallback executionLogCallback) {
    Secret secretMap = new SecretBuilder()
                           .withNewMetadata()
                           .withName(secretMapName)
                           .withNamespace(setupParams.getNamespace())
                           .withLabels(controllerLabels)
                           .endMetadata()
                           .build();

    Map<String, String> secretData = new HashMap<>();

    Map<String, String> encryptedServiceVars =
        safeDisplayServiceVariables.entrySet()
            .stream()
            .filter(entry -> SECRET_MASK.equals(entry.getValue()))
            .collect(toMap(Entry::getKey, entry -> serviceVariables.get(entry.getKey())));

    if (isNotEmpty(encryptedServiceVars)) {
      secretData.putAll(encryptedServiceVars);
    }

    if (isNotEmpty(setupParams.getEncryptedConfigFiles())) {
      secretData.putAll(setupParams.getEncryptedConfigFiles().stream().collect(toMap(sa -> sa[0], sa -> sa[1])));
    }

    if (isEmpty(secretData)) {
      secretMap = null;
    }

    if (secretMap != null) {
      executionLogCallback.saveExecutionLog("Creating secret map:\n\n"
              + toDisplayYaml(new SecretBuilder()
                                  .withMetadata(secretMap.getMetadata())
                                  .withStringData(secretData.entrySet().stream().collect(
                                      toMap(Entry::getKey, entry -> SECRET_MASK)))
                                  .build()),
          LogLevel.INFO);
      secretMap.setStringData(secretData);
      kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails, secretMap);
    }
    return secretMap;
  }

  private ConfigMap prepareConfigMap(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesSetupParams setupParams, String configMapName, Map<String, String> controllerLabels,
      Map<String, String> safeDisplayServiceVariables, ExecutionLogCallback executionLogCallback) {
    ConfigMap configMap;
    if (isNotBlank(setupParams.getConfigMapYaml())) {
      try {
        configMap = KubernetesHelper.loadYaml(setupParams.getConfigMapYaml());
        ObjectMeta configMapMeta = Optional.ofNullable(configMap.getMetadata()).orElse(new ObjectMeta());
        configMapMeta.setName(configMapName);
        configMapMeta.setNamespace(setupParams.getNamespace());
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
                      .withName(configMapName)
                      .withNamespace(setupParams.getNamespace())
                      .withLabels(controllerLabels)
                      .endMetadata()
                      .build();

      Map<String, String> data = new HashMap<>();

      if (isNotEmpty(safeDisplayServiceVariables)) {
        data.putAll(safeDisplayServiceVariables.entrySet()
                        .stream()
                        .filter(entry -> !SECRET_MASK.equals(entry.getValue()))
                        .collect(toMap(Entry::getKey, Entry::getValue)));
      }

      if (isNotEmpty(setupParams.getPlainConfigFiles())) {
        data.putAll(setupParams.getPlainConfigFiles().stream().collect(toMap(sa -> sa[0], sa -> sa[1])));
      }
      configMap.setData(data);
    }

    if (isEmpty(configMap.getData())) {
      configMap = null;
    }
    if (configMap != null) {
      executionLogCallback.saveExecutionLog("Creating config map:\n\n" + toDisplayYaml(configMap));
      kubernetesContainerService.createOrReplaceConfigMap(kubernetesConfig, encryptedDataDetails, configMap);
    }
    return configMap;
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
      String kubernetesServiceName, String containerServiceName, ExecutionLogCallback executionLogCallback) {
    int port = isNotEmpty(service.getSpec().getPorts()) ? service.getSpec().getPorts().get(0).getPort() : 80;
    try {
      Ingress ingress =
          KubernetesHelper.loadYaml(setupParams.getIngressYaml()
                                        .replaceAll(SERVICE_NAME_PLACEHOLDER_REGEX, kubernetesServiceName)
                                        .replaceAll(SERVICE_PORT_PLACEHOLDER_REGEX, Integer.toString(port))
                                        .replaceAll(CONFIG_MAP_NAME_PLACEHOLDER_REGEX, containerServiceName)
                                        .replaceAll(SECRET_MAP_NAME_PLACEHOLDER_REGEX, containerServiceName));
      ingress.getMetadata().setName(kubernetesServiceName);
      Map<String, String> labels = ingress.getMetadata().getLabels();
      if (labels == null) {
        labels = new HashMap<>();
      }
      labels.putAll(service.getSpec().getSelector());
      ingress.getMetadata().setLabels(labels);
      executionLogCallback.saveExecutionLog("Setting ingress:\n\n" + toDisplayYaml(ingress));
      return ingress;
    } catch (IOException e) {
      executionLogCallback.saveExecutionLog(
          "Error reading Ingress from yaml: " + kubernetesServiceName, LogLevel.ERROR);
    }
    return null;
  }

  private List<String> getActiveAutoscalers(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName,
      ExecutionLogCallback executionLogCallback, boolean isStatefulSet) {
    String controllerNamePrefix = getPrefixFromControllerName(containerServiceName, isStatefulSet);
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
      horizontalPodAutoscaler =
          getCustomMetricHorizontalPodAutoscaler(autoscalerName, namespace, serviceLabels, setupParams);
    } else {
      executionLogCallback.saveExecutionLog(
          format("Setting autoscaler min instances %d, max instances %d, with target CPU utilization %d%%",
              setupParams.getMinAutoscaleInstances(), setupParams.getMaxAutoscaleInstances(),
              setupParams.getTargetCpuUtilizationPercentage()),
          LogLevel.INFO);

      horizontalPodAutoscaler = getBasicHorizontalPodAutoscaler(autoscalerName, namespace, serviceLabels, setupParams);
    }

    executionLogCallback.saveExecutionLog(
        "Creating horizontal pod autoscaler:\n\n" + toDisplayYaml(horizontalPodAutoscaler));
    return horizontalPodAutoscaler;
  }

  private void disableAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String autoscaler, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Disabling autoscaler " + autoscaler);
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
  }

  private HorizontalPodAutoscaler getCustomMetricHorizontalPodAutoscaler(
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
      throw new WingsException("Error while loading custom yaml for horizontal pod autoscaler", e);
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

  private IstioResource createRouteRuleDefinition(KubernetesSetupParams setupParams, String kubernetesServiceName,
      Map<String, String> serviceLabels, Map<String, Integer> activeControllers, boolean isStatefulSet,
      ExecutionLogCallback executionLogCallback) {
    RouteRuleSpecNested<IstioResourceBuilder> routeRuleSpecNested = new IstioResourceBuilder()
                                                                        .withNewMetadata()
                                                                        .withName(kubernetesServiceName)
                                                                        .withNamespace(setupParams.getNamespace())
                                                                        .withLabels(serviceLabels)
                                                                        .endMetadata()
                                                                        .withNewRouteRuleSpec()
                                                                        .withNewDestination()
                                                                        .withName(kubernetesServiceName)
                                                                        .withNamespace(setupParams.getNamespace())
                                                                        .endDestination();
    int totalInstances = activeControllers.values().stream().mapToInt(Integer::intValue).sum();
    for (String controller : activeControllers.keySet()) {
      Optional<Integer> revision = getRevisionFromControllerName(controller, isStatefulSet);
      if (revision.isPresent()) {
        int weight = (int) Math.round((activeControllers.get(controller) * 100.0) / totalInstances);
        if (weight > 0) {
          routeRuleSpecNested.addNewRoute()
              .addToLabels(HARNESS_REVISION, revision.get().toString())
              .withWeight(weight)
              .endRoute();
        }
      }
    }
    IstioResource routeRule = routeRuleSpecNested.endRouteRuleSpec().build();
    executionLogCallback.saveExecutionLog("Creating istio route rule:\n\n" + toDisplayYaml(routeRule));
    return routeRule;
  }

  private void listDaemonSetContainerInfosWhenReady(List<EncryptedDataDetail> encryptedDataDetails,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback, KubernetesConfig kubernetesConfig,
      String containerServiceName, List<Pod> originalPods, long startTime, boolean isRollback) {
    int desiredCount = kubernetesContainerService.getNodes(kubernetesConfig, encryptedDataDetails).getItems().size();
    int previousCount =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, containerServiceName) != null
        ? desiredCount
        : 0;
    List<ContainerInfo> containerInfos = kubernetesContainerService.getContainerInfosWhenReady(kubernetesConfig,
        encryptedDataDetails, containerServiceName, previousCount, desiredCount, serviceSteadyStateTimeout,
        originalPods, true, executionLogCallback, true, startTime);

    boolean allContainersSuccess =
        containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
    if (containerInfos.size() != desiredCount || !allContainersSuccess) {
      if (containerInfos.size() != desiredCount) {
        String message = format("Expected data for %d %s but got %d", desiredCount, plural("container", desiredCount),
            containerInfos.size());
        executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
      }
      if (!allContainersSuccess) {
        List<ContainerInfo> failed =
            containerInfos.stream().filter(info -> info.getStatus() != ContainerInfo.Status.SUCCESS).collect(toList());
        String message = format("The following %s did not have success status: %s", plural("container", failed.size()),
            failed.stream().map(ContainerInfo::getContainerId).collect(toList()));
        executionLogCallback.saveExecutionLog(message, LogLevel.ERROR);
      }
      executionLogCallback.saveExecutionLog(
          format("Completed operation with errors\n%s\n", DASH_STRING), LogLevel.ERROR);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE)
          .addParam("message", "DaemonSet pods failed to reach desired count");
    }
    executionLogCallback.saveExecutionLog("\nDaemonSet Container IDs:");
    containerInfos.forEach(
        info -> executionLogCallback.saveExecutionLog("  " + info.getHostName() + " - " + info.getContainerId()));
    executionLogCallback.saveExecutionLog("");
    if (!isRollback) {
      executionLogCallback.saveExecutionLog(format("Completed operation\n%s\n", DASH_STRING));
    }
  }

  private void performDaemonSetRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig) {
    String daemonSetName = setupParams.getControllerNamePrefix();
    String daemonSetYaml = setupParams.getPreviousDaemonSetYaml();
    long startTime = clock.millis();
    List<Pod> originalPods =
        kubernetesContainerService.getRunningPods(kubernetesConfig, encryptedDataDetails, daemonSetName);
    if (isNotBlank(daemonSetYaml)) {
      try {
        DaemonSet daemonSet = KubernetesHelper.loadYaml(daemonSetYaml);
        executionLogCallback.saveExecutionLog("Rolling back DaemonSet " + daemonSetName);
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
        listDaemonSetContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
            executionLogCallback, kubernetesConfig, daemonSetName, originalPods, startTime, true);
      } catch (IOException e) {
        executionLogCallback.saveExecutionLog("Error reading DaemonSet from yaml: " + daemonSetName, LogLevel.ERROR);
      }
    } else {
      executionLogCallback.saveExecutionLog(
          "DaemonSet " + daemonSetName + " did not exist previously. Deleting on rollback");
      HasMetadata daemonSet =
          kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, daemonSetName);
      Map<String, String> labels = daemonSet.getMetadata().getLabels();
      kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, daemonSetName);
      kubernetesContainerService.waitForPodsToStop(kubernetesConfig, encryptedDataDetails, labels,
          setupParams.getServiceSteadyStateTimeout(), originalPods, startTime, executionLogCallback);
    }
  }

  private void performAutoscalerRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig) {
    List<String> autoscalerNames = setupParams.getPreviousActiveAutoscalers();
    if (isNotEmpty(autoscalerNames)) {
      for (String autoscalerName : autoscalerNames) {
        executionLogCallback.saveExecutionLog("Enabling autoscaler " + autoscalerName);
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
    return isBlank(yamlConfig) ? ContainerApiVersions.KUBERNETES_V1.getVersionName()
                               : ContainerApiVersions.KUBERNETES_V2_BETA1.getVersionName();
  }

  private Secret createRegistrySecret(
      String secretName, String namespace, ImageDetails imageDetails, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Setting image pull secret " + secretName);
    String credentialData = format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
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
      executionLogCallback.saveExecutionLog("Waiting for service " + serviceName + " load balancer to be ready");
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
            format("Timed out waiting for service [%s] load balancer to be ready", serviceName), LogLevel.ERROR);
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
        format("Service [%s] load balancer is ready with endpoint [%s]", serviceName, loadBalancerEndpoint),
        LogLevel.INFO);
    return loadBalancerEndpoint;
  }

  private String lastController(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String controllerNamePrefix, boolean isStatefulSet) {
    String versionSeparator = isStatefulSet ? DASH : DOT;

    final HasMetadata[] lastReplicationController = {null};
    final AtomicInteger lastRevision = new AtomicInteger();
    kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix + versionSeparator))
        .forEach(ctrl -> {
          Optional<Integer> revision = getRevisionFromControllerName(ctrl.getMetadata().getName(), isStatefulSet);
          if (revision.isPresent() && (lastReplicationController[0] == null || revision.get() > lastRevision.get())) {
            lastReplicationController[0] = ctrl;
            lastRevision.set(revision.get());
          }
        });
    return lastReplicationController[0] != null ? lastReplicationController[0].getMetadata().getName() : null;
  }

  private HasMetadata createKubernetesControllerDefinition(KubernetesContainerTask kubernetesContainerTask,
      String controllerName, Map<String, String> controllerLabels, String namespace, ImageDetails imageDetails,
      String registrySecretName, ConfigMap configMap, Secret secretMap, ExecutionLogCallback executionLogCallback) {
    String containerName = KubernetesConvention.getContainerName(imageDetails.getName());
    String imageNameTag = imageDetails.getName() + ":" + imageDetails.getTag();

    String configMapName = configMap != null ? configMap.getMetadata().getName() : "no-config-map";
    String secretMapName = secretMap != null ? secretMap.getMetadata().getName() : "no-secret-map";

    HasMetadata controller = kubernetesContainerTask.createController(
        containerName, imageNameTag, registrySecretName, configMapName, secretMapName);

    KubernetesHelper.setName(controller, controllerName);
    KubernetesHelper.setNamespace(controller, namespace);
    KubernetesHelper.getOrCreateLabels(controller).putAll(controllerLabels);

    configureTypeSpecificSpecs(controllerLabels, controller, configMap, secretMap, executionLogCallback);

    executionLogCallback.saveExecutionLog("Creating controller:\n\n" + toDisplayYaml(controller));
    return controller;
  }

  private void configureTypeSpecificSpecs(Map<String, String> controllerLabels, HasMetadata kubernetesObj,
      ConfigMap configMap, Secret secretMap, ExecutionLogCallback executionLogCallback) {
    PodTemplateSpec podTemplateSpec = null;
    if (kubernetesObj instanceof ReplicationController) {
      ReplicationControllerSpec rcSpec = ((ReplicationController) kubernetesObj).getSpec();
      rcSpec.setSelector(controllerLabels);
      rcSpec.setReplicas(0);
      podTemplateSpec = rcSpec.getTemplate();
    } else if (kubernetesObj instanceof Deployment) {
      DeploymentSpec depSpec = ((Deployment) kubernetesObj).getSpec();
      depSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      depSpec.setReplicas(0);
      podTemplateSpec = depSpec.getTemplate();
    } else if (kubernetesObj instanceof DaemonSet) {
      DaemonSetSpec dsSpec = ((DaemonSet) kubernetesObj).getSpec();
      dsSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      podTemplateSpec = dsSpec.getTemplate();
    } else if (kubernetesObj instanceof ReplicaSet) {
      ReplicaSetSpec repSetSpec = ((ReplicaSet) kubernetesObj).getSpec();
      repSetSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      repSetSpec.setReplicas(0);
      podTemplateSpec = repSetSpec.getTemplate();
    } else if (kubernetesObj instanceof StatefulSet) {
      StatefulSetSpec stateSetSpec = ((StatefulSet) kubernetesObj).getSpec();
      stateSetSpec.setSelector(new LabelSelectorBuilder().withMatchLabels(controllerLabels).build());
      stateSetSpec.setReplicas(0);
      podTemplateSpec = stateSetSpec.getTemplate();
    }

    Preconditions.checkNotNull(podTemplateSpec, "Pod template spec is missing in controller definition");
    Preconditions.checkNotNull(
        podTemplateSpec.getSpec(), "Pod spec in pod template spec is missing in controller definition");

    if (podTemplateSpec.getMetadata() == null) {
      podTemplateSpec.setMetadata(new ObjectMeta());
    }

    Map<String, String> labels = podTemplateSpec.getMetadata().getLabels();
    if (labels == null) {
      labels = new HashMap<>();
      podTemplateSpec.getMetadata().setLabels(labels);
    }
    podTemplateSpec.getMetadata().getLabels().putAll(controllerLabels);

    Map<String, EnvVar> secretEnvVars = new HashMap<>();

    if (secretMap != null) {
      for (String key : secretMap.getStringData().keySet()) {
        if (envVarPattern.matcher(key).matches()) {
          EnvVarSource varSource = new EnvVarSourceBuilder()
                                       .withNewSecretKeyRef()
                                       .withName(secretMap.getMetadata().getName())
                                       .withKey(key)
                                       .endSecretKeyRef()
                                       .build();
          secretEnvVars.put(key, new EnvVarBuilder().withName(key).withValueFrom(varSource).build());
        } else {
          String msg =
              format("Key name [%s] from secret map is not a valid environment variable name. Skipping...", key);
          executionLogCallback.saveExecutionLog(msg, LogLevel.WARN);
        }
      }
    }

    for (Container container : podTemplateSpec.getSpec().getContainers()) {
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
      if (isNotEmpty(secretEnvVars)) {
        Map<String, EnvVar> containerEnvVars = new HashMap<>();
        if (container.getEnv() != null) {
          container.getEnv().forEach(envVar -> containerEnvVars.put(envVar.getName(), envVar));
        }
        containerEnvVars.putAll(secretEnvVars);
        container.setEnv(new ArrayList<>(containerEnvVars.values()));
      }
    }
  }

  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(String serviceName, String namespace,
      Map<String, String> serviceLabels, KubernetesSetupParams setupParams, ExecutionLogCallback executionLogCallback) {
    ServiceSpecBuilder spec =
        new ServiceSpecBuilder().withSelector(serviceLabels).withType(setupParams.getServiceType().name());

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
      spec.withPorts(ImmutableList.of(servicePort.build()));

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

    Service service = new ServiceBuilder()
                          .withNewMetadata()
                          .withName(serviceName)
                          .withNamespace(namespace)
                          .addToLabels(serviceLabels)
                          .endMetadata()
                          .withSpec(spec.build())
                          .build();
    executionLogCallback.saveExecutionLog("Setting service:\n\n" + toDisplayYaml(service));
    return service;
  }

  private io.fabric8.kubernetes.api.model.Service createdServiceDefinitionFromYaml(String serviceName, String namespace,
      Map<String, String> serviceLabels, KubernetesSetupParams setupParams, ExecutionLogCallback executionLogCallback) {
    try {
      Service service = KubernetesHelper.loadYaml(setupParams.getServiceYaml());
      service.getMetadata().setLabels(serviceLabels);
      service.getMetadata().setName(serviceName);
      service.getMetadata().setNamespace(namespace);
      service.getSpec().setSelector(serviceLabels);
      executionLogCallback.saveExecutionLog("Setting service:\n\n" + toDisplayYaml(service));
      return service;
    } catch (IOException e) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", e.getMessage());
    }
  }

  private void cleanup(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String containerServiceName, ExecutionLogCallback executionLogCallback, boolean isStatefulSet) {
    Optional<Integer> revision = getRevisionFromControllerName(containerServiceName, isStatefulSet);
    if (revision.isPresent() && revision.get() >= KEEP_N_REVISIONS) {
      int minRevisionToKeep = revision.get() - KEEP_N_REVISIONS + 1;
      String controllerNamePrefix = getPrefixFromControllerName(containerServiceName, isStatefulSet);
      kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
          .stream()
          .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
          .filter(ctrl -> kubernetesContainerService.getControllerPodCount(ctrl) == 0)
          .forEach(ctrl -> {
            String controllerName = ctrl.getMetadata().getName();
            Optional<Integer> ctrlRevision = getRevisionFromControllerName(controllerName, isStatefulSet);
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

  private void printIngressRules(Ingress ingress, ExecutionLogCallback executionLogCallback) {
    String path;
    String port;
    String serviceName;
    String host;
    try {
      for (IngressRule ingressRule : ingress.getSpec().getRules()) {
        HTTPIngressPath httpIngressPath = ingressRule.getHttp().getPaths().get(0);
        path = httpIngressPath.getPath();
        port = httpIngressPath.getBackend().getServicePort().getIntVal().toString();
        serviceName = httpIngressPath.getBackend().getServiceName();
        host = ingressRule.getHost();
        executionLogCallback.saveExecutionLog(
            "Ingress Rule: " + (isNotBlank(host) ? host : "") + ":" + port + path + " → " + serviceName);
      }
    } catch (Exception e) {
      logger.error("Couldn't get path from ingress rule.", e);
      executionLogCallback.saveExecutionLog("Error getting Ingress rule - " + Misc.getMessage(e), LogLevel.WARN);
    }
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
