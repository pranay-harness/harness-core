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
import static software.wings.beans.command.ContainerApiVersions.KUBERNETES_V1;
import static software.wings.beans.command.ContainerResizeCommandUnit.DASH_STRING;
import static software.wings.beans.container.KubernetesContainerTask.CONFIG_MAP_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.KubernetesContainerTask.SECRET_MAP_NAME_PLACEHOLDER_REGEX;
import static software.wings.beans.container.KubernetesServiceType.None;
import static software.wings.common.Constants.HARNESS_APP;
import static software.wings.common.Constants.HARNESS_ENV;
import static software.wings.common.Constants.HARNESS_KUBERNETES_APP_LABEL_KEY;
import static software.wings.common.Constants.HARNESS_KUBERNETES_ENV_LABEL_KEY;
import static software.wings.common.Constants.HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY;
import static software.wings.common.Constants.HARNESS_KUBERNETES_MANAGED_LABEL_KEY;
import static software.wings.common.Constants.HARNESS_KUBERNETES_REVISION_LABEL_KEY;
import static software.wings.common.Constants.HARNESS_KUBERNETES_SERVICE_LABEL_KEY;
import static software.wings.common.Constants.HARNESS_REVISION;
import static software.wings.common.Constants.HARNESS_SERVICE;
import static software.wings.common.Constants.SECRET_MASK;
import static software.wings.exception.WingsException.USER;
import static software.wings.service.impl.KubernetesHelperService.printRouteRuleWeights;
import static software.wings.service.impl.KubernetesHelperService.toDisplayYaml;
import static software.wings.service.impl.KubernetesHelperService.toYaml;
import static software.wings.utils.KubernetesConvention.getBlueGreenIngressName;
import static software.wings.utils.KubernetesConvention.getKubernetesRegistrySecretName;
import static software.wings.utils.KubernetesConvention.getKubernetesServiceName;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getPrimaryServiceName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;
import static software.wings.utils.KubernetesConvention.getStageServiceName;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import software.wings.beans.command.KubernetesYamlConfig.KubernetesYamlConfigBuilder;
import software.wings.beans.container.ContainerDefinition;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesServiceSpecification;
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

import java.io.IOException;
import java.time.Clock;
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
import java.util.concurrent.atomic.AtomicReference;
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
  @Transient private static final String LOAD_BALANCER = "LoadBalancer";
  @Transient private static final String NODE_PORT = "NodePort";
  @Transient private static final String POLICY_LOCAL = "Local";

  @Transient private static final String SERVICE_NAME_PLACEHOLDER_REGEX = "\\$\\{SERVICE_NAME}";
  @Transient private static final String SERVICE_PORT_PLACEHOLDER_REGEX = "\\$\\{SERVICE_PORT}";

  @Transient private static final String PRIMARY_SERVICE_NAME_PLACEHOLDER_REGEX = "\\$\\{PRIMARY_SERVICE_NAME}";
  @Transient private static final String PRIMARY_SERVICE_PORT_PLACEHOLDER_REGEX = "\\$\\{PRIMARY_SERVICE_PORT}";

  @Transient private static final String STAGE_SERVICE_NAME_PLACEHOLDER_REGEX = "\\$\\{STAGE_SERVICE_NAME}";
  @Transient private static final String STAGE_SERVICE_PORT_PLACEHOLDER_REGEX = "\\$\\{STAGE_SERVICE_PORT}";

  @Inject private transient GkeClusterService gkeClusterService;
  @Inject private transient KubernetesContainerService kubernetesContainerService;
  @Inject private transient TimeLimiter timeLimiter;
  @Inject private transient Clock clock;
  @Inject private transient AzureHelperService azureHelperService;

  private Map<String, String> harnessAnnotations;
  private Map<String, String> lookupLabels;
  private int currentRevision;
  private boolean isNotVersioned;
  private String currentNamePrefix;

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

    StringBuffer summaryOutput = new StringBuffer();

    try {
      KubernetesSetupParams setupParams = (KubernetesSetupParams) containerSetupParams;

      harnessAnnotations = getHarnessAnnotations(setupParams);
      lookupLabels = ImmutableMap.of(HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY,
          KubernetesConvention.getNormalizedInfraMappingIdLabelValue(setupParams.getInfraMappingId()));

      KubernetesConfig kubernetesConfig;
      List<EncryptedDataDetail> encryptedDataDetails;
      if (cloudProviderSetting.getValue() instanceof KubernetesConfig) {
        kubernetesConfig = (KubernetesConfig) cloudProviderSetting.getValue();
        encryptedDataDetails = edd;
      } else if (cloudProviderSetting.getValue() instanceof KubernetesClusterConfig) {
        KubernetesClusterConfig config = (KubernetesClusterConfig) cloudProviderSetting.getValue();
        String delegateName = System.getenv().get("DELEGATE_NAME");
        if (config.isUseKubernetesDelegate() && !config.getDelegateName().equals(delegateName)) {
          throw new InvalidRequestException(format("Kubernetes delegate name [%s] doesn't match "
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

      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) setupParams.getContainerTask();
      if (kubernetesContainerTask == null) {
        kubernetesContainerTask = new KubernetesContainerTask();
        ContainerDefinition containerDefinition = ContainerDefinition.builder().memory(256).cpu(1).build();
        kubernetesContainerTask.setContainerDefinitions(Lists.newArrayList(containerDefinition));
      }

      boolean isStatefulSet = kubernetesContainerTask.checkStatefulSet();
      boolean isDaemonSet = kubernetesContainerTask.checkDaemonSet();
      isNotVersioned = isDaemonSet || isStatefulSet;
      boolean useDashInHostname = setupParams.isUseDashInHostname();
      currentNamePrefix = setupParams.getControllerNamePrefix();

      String lastCtrlName;
      if (setupParams.isUseNewLabelMechanism()) {
        HasMetadata lastDeployment = getLastDeployment(kubernetesConfig, encryptedDataDetails, setupParams);
        currentRevision = isNotVersioned ? 0 : getRevisionNumber(lastDeployment) + 1;
        lastCtrlName = lastDeployment != null ? lastDeployment.getMetadata().getName() : null;
      } else {
        lastCtrlName = lastController(
            kubernetesConfig, encryptedDataDetails, setupParams.getControllerNamePrefix(), useDashInHostname);
        currentRevision = getRevisionFromControllerName(lastCtrlName, useDashInHostname).orElse(-1) + 1;
      }

      String containerServiceName = isNotVersioned
          ? isStatefulSet ? KubernetesConvention.getKubernetesServiceName(setupParams.getControllerNamePrefix())
                          : setupParams.getControllerNamePrefix()
          : KubernetesConvention.getControllerName(
                setupParams.getControllerNamePrefix(), currentRevision, useDashInHostname);

      Map<String, String> controllerLabels = isNotVersioned
          ? getLabels(setupParams)
          : getLabelsWithRevision(setupParams, Integer.toString(currentRevision));

      String dockerImageName = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();

      summaryOutput.append(format("%nCluster Name: %s", setupParams.getClusterName()));
      summaryOutput.append(format("%nController Name: %s", containerServiceName));
      summaryOutput.append(format("%nDocker Image Name: %s", dockerImageName));

      if (setupParams.isRollback()) {
        executionLogCallback.saveExecutionLog("Rolling back setup");
        if (isNotVersioned) {
          performYamlRollback(
              encryptedDataDetails, executionLogCallback, setupParams, kubernetesConfig, containerServiceName);
        } else if (setupParams.getPreviousYamlConfig() != null
            && isNotBlank(setupParams.getPreviousYamlConfig().getAutoscalerYaml())) {
          performAutoscalerRollback(kubernetesConfig, encryptedDataDetails, executionLogCallback, setupParams);
        }
        executionLogCallback.saveExecutionLog("Rollback complete");
        executionLogCallback.saveExecutionLog(DASH_STRING + "\n");

        context.setCommandExecutionData(
            commandExecutionDataBuilder.containerServiceName(setupParams.getControllerNamePrefix()).build());

        return CommandExecutionStatus.SUCCESS;
      }

      if (setupParams.isBlueGreen()) {
        executionLogCallback.saveExecutionLog("Blue/Green Service Setup\n");

        validateBlueGreenConfig(setupParams);

        cleanupStageDeployment(
            kubernetesConfig, encryptedDataDetails, setupParams, executionLogCallback, useDashInHostname);
      }

      String registrySecretName = getKubernetesRegistrySecretName(setupParams.getImageDetails());
      Secret registrySecret = createRegistrySecret(registrySecretName, setupParams.getNamespace(),
          setupParams.getImageDetails(), controllerLabels, executionLogCallback);
      kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails, registrySecret);

      KubernetesYamlConfigBuilder yamlConfigBuilder = KubernetesYamlConfig.builder();
      List<Pod> originalPods = null;
      Map<String, Integer> activeServiceCounts = new HashMap<>();
      Map<String, Integer> trafficWeights = new HashMap<>();
      String previousAutoscalerYaml = null;
      String lastAutoscaler = null;

      if (isNotVersioned) {
        yamlConfigBuilder.controllerYaml(
            getControllerYaml(kubernetesConfig, encryptedDataDetails, containerServiceName));
        originalPods =
            kubernetesContainerService.getRunningPods(kubernetesConfig, encryptedDataDetails, containerServiceName);
        yamlConfigBuilder.configMapYaml(getConfigMapYaml(kubernetesConfig, encryptedDataDetails, containerServiceName));
        yamlConfigBuilder.secretMapYaml(getSecretMapYaml(kubernetesConfig, encryptedDataDetails, containerServiceName));
        previousAutoscalerYaml = getAutoscalerYaml(kubernetesConfig, encryptedDataDetails, containerServiceName);
        if (isNotBlank(previousAutoscalerYaml)) {
          lastAutoscaler = containerServiceName;
        }
      } else {
        if (setupParams.isUseNewLabelMechanism()) {
          activeServiceCounts = kubernetesContainerService.getActiveServiceCountsWithLabels(
              kubernetesConfig, encryptedDataDetails, lookupLabels);
        } else {
          activeServiceCounts = kubernetesContainerService.getActiveServiceCounts(
              kubernetesConfig, encryptedDataDetails, containerServiceName, useDashInHostname);
        }

        trafficWeights = kubernetesContainerService.getTrafficWeights(
            kubernetesConfig, encryptedDataDetails, containerServiceName, useDashInHostname);
        if (isNotBlank(lastCtrlName)) {
          String previousAutoscalerName = lastAutoscaler(
              kubernetesConfig, encryptedDataDetails, setupParams.getControllerNamePrefix(), useDashInHostname);
          if (isNotBlank(previousAutoscalerName)) {
            previousAutoscalerYaml = getAutoscalerYaml(kubernetesConfig, encryptedDataDetails, previousAutoscalerName);
            if (isNotBlank(previousAutoscalerYaml)) {
              lastAutoscaler = previousAutoscalerName;
            }
          }
        }
      }

      yamlConfigBuilder.autoscalerYaml(previousAutoscalerYaml);

      KubernetesYamlConfig yamlConfig = yamlConfigBuilder.build();

      commandExecutionDataBuilder.containerServiceName(containerServiceName)
          .previousYamlConfig(yamlConfig)
          .activeServiceCounts(integerMapToListOfStringArray(activeServiceCounts))
          .trafficWeights(integerMapToListOfStringArray(trafficWeights));

      // Setup config map
      ConfigMap configMap = prepareConfigMap(kubernetesConfig, encryptedDataDetails, setupParams, containerServiceName,
          controllerLabels, safeDisplayServiceVariables, executionLogCallback);

      // Setup secret map
      Secret secretMap = prepareSecretMap(kubernetesConfig, encryptedDataDetails, setupParams, containerServiceName,
          controllerLabels, safeDisplayServiceVariables, serviceVariables, executionLogCallback);

      long replaceControllerStartTime = clock.millis();

      HasMetadata definition = createKubernetesControllerDefinition(kubernetesContainerTask, containerServiceName,
          controllerLabels, setupParams, registrySecretName, configMap, secretMap, originalPods, executionLogCallback);

      if (definition instanceof StatefulSet) {
        HasMetadata existing = kubernetesContainerService.getController(
            kubernetesConfig, encryptedDataDetails, definition.getMetadata().getName());
        if (existing != null && existing.getKind().equals("StatefulSet")) {
          StatefulSet newDefinition = (StatefulSet) definition;
          StatefulSet mergedDefinition = (StatefulSet) existing;
          mergedDefinition.getSpec().setReplicas(newDefinition.getSpec().getReplicas());
          mergedDefinition.getSpec().setTemplate(newDefinition.getSpec().getTemplate());
          mergedDefinition.getSpec().getTemplate().getMetadata().setLabels(
              mergedDefinition.getSpec().getSelector().getMatchLabels());
          mergedDefinition.getSpec().setUpdateStrategy(newDefinition.getSpec().getUpdateStrategy());
          definition = mergedDefinition;
        }
      }

      executionLogCallback.saveExecutionLog("Creating controller:\n\n" + toDisplayYaml(definition));
      // Setup controller
      HasMetadata controller =
          kubernetesContainerService.createOrReplaceController(kubernetesConfig, encryptedDataDetails, definition);

      // Delete old autoscaler
      if (isNotBlank(lastAutoscaler)) {
        kubernetesContainerService.deleteAutoscaler(kubernetesConfig, encryptedDataDetails, lastAutoscaler);
      }

      // Create new autoscaler
      HorizontalPodAutoscaler hpa = prepareHorizontalPodAutoscaler(setupParams, containerServiceName,
          controller.getKind(), controller.getApiVersion(), controllerLabels, executionLogCallback);

      commandExecutionDataBuilder.autoscalerYaml(toYaml(hpa));

      if (configMap != null) {
        summaryOutput.append(format("%nConfig Map: %s", configMap.getMetadata().getName()));
      }
      if (secretMap != null) {
        summaryOutput.append(format("%nSecret Map: %s", secretMap.getMetadata().getName()));
      }
      if (hpa != null) {
        summaryOutput.append(format("%nHorizontal Pod Autoscaler: %s", hpa.getMetadata().getName()));
      }

      setupServiceAndIngressAndIstioRouteRule(setupParams, kubernetesConfig, encryptedDataDetails, containerServiceName,
          activeServiceCounts, executionLogCallback, summaryOutput);

      setupServiceAndIngressForBlueGreen(setupParams, kubernetesConfig, encryptedDataDetails, containerServiceName,
          Integer.toString(currentRevision), executionLogCallback, summaryOutput);

      if (isNotVersioned) {
        listContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
            executionLogCallback, kubernetesConfig, containerServiceName, originalPods, replaceControllerStartTime,
            false);
        if (isStatefulSet) {
          if (hpa != null) {
            executionLogCallback.saveExecutionLog("Enabling autoscaler " + containerServiceName, LogLevel.INFO);
            hpa = kubernetesContainerService.createOrReplaceAutoscaler(
                kubernetesConfig, encryptedDataDetails, toYaml(hpa));
            if (hpa != null) {
              String hpaName = hpa.getMetadata().getName();
              executionLogCallback.saveExecutionLog(
                  "Created horizontal pod autoscaler " + hpaName + ":\n\n" + toDisplayYaml(hpa));
            }
          }
        }
      } else {
        downsizeOldOrUnhealthy(kubernetesConfig, encryptedDataDetails, containerServiceName, setupParams,
            executionLogCallback, useDashInHostname);
        if (setupParams.isUseNewLabelMechanism()) {
          cleanupWithLabels(kubernetesConfig, encryptedDataDetails, executionLogCallback);

        } else {
          cleanup(
              kubernetesConfig, encryptedDataDetails, containerServiceName, executionLogCallback, useDashInHostname);
        }
      }

      executionLogCallback.saveExecutionLog("\n\nSummary:");
      executionLogCallback.saveExecutionLog(summaryOutput.toString());

      return CommandExecutionStatus.SUCCESS;
    } catch (Exception ex) {
      logger.error(Misc.getMessage(ex), ex);
      Misc.logAllMessages(ex, executionLogCallback);
      return CommandExecutionStatus.FAILURE;
    } finally {
      context.setCommandExecutionData(commandExecutionDataBuilder.build());
    }
  }

  private void validateBlueGreenConfig(KubernetesSetupParams setupParams) {
    if (setupParams.getServiceType() != null && setupParams.getServiceType() != None) {
      throw new InvalidRequestException("Service cannot be specified for Blue/Green deployment setup");
    }

    if (setupParams.getBlueGreenConfig() == null) {
      throw new InvalidRequestException("BlueGreenConfig is not specified");
    }

    if (setupParams.getBlueGreenConfig().getPrimaryService() == null
        || setupParams.getBlueGreenConfig().getPrimaryService().getServiceType() == None) {
      throw new InvalidRequestException("PrimaryService is not specified in BlueGreenConfig");
    }

    if (setupParams.getBlueGreenConfig().getStageService() == null
        || setupParams.getBlueGreenConfig().getStageService().getServiceType() == None) {
      throw new InvalidRequestException("StageService is not specified in BlueGreenConfig");
    }
  }

  private void setupServiceAndIngressAndIstioRouteRule(KubernetesSetupParams setupParams,
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName,
      Map<String, Integer> activeServiceCounts, ExecutionLogCallback executionLogCallback, StringBuffer summaryOutput) {
    String kubernetesServiceName = getKubernetesServiceName(setupParams.getControllerNamePrefix());
    Map<String, String> labels = getLabels(setupParams);

    KubernetesServiceSpecification spec = new KubernetesServiceSpecification();
    spec.setServiceType(setupParams.getServiceType());
    spec.setClusterIP(setupParams.getClusterIP());
    spec.setExternalIPs(setupParams.getExternalIPs());
    spec.setExternalName(setupParams.getExternalName());
    spec.setLoadBalancerIP(setupParams.getLoadBalancerIP());
    spec.setNodePort(setupParams.getNodePort());
    spec.setPort(setupParams.getPort());
    spec.setProtocol(setupParams.getProtocol());
    spec.setTargetPort(setupParams.getTargetPort());
    spec.setServiceYaml(setupParams.getServiceYaml());

    if (!setupParams.isBlueGreen() && setupParams.getServiceType() != null && setupParams.getServiceType() != None) {
      executionLogCallback.saveExecutionLog(
          format("Setting Service with name %s with Type %s", kubernetesServiceName, setupParams.getServiceType()),
          LogLevel.INFO);
    }

    Map<String, String> selectors =
        setupParams.isUseNewLabelMechanism() ? getNewLabelSelectors(setupParams) : getLabelSelectors(setupParams);

    Service service =
        prepareService(spec, kubernetesConfig, encryptedDataDetails, kubernetesServiceName, setupParams.getNamespace(),
            false, selectors, labels, setupParams.getServiceSteadyStateTimeout(), executionLogCallback, summaryOutput);

    String ingressYaml = "";

    if (service != null && setupParams.isUseIngress()) {
      int port = (service.getSpec() != null && isNotEmpty(service.getSpec().getPorts()))
          ? service.getSpec().getPorts().get(0).getPort()
          : 80;
      ingressYaml = setupParams.getIngressYaml()
                        .replaceAll(SERVICE_NAME_PLACEHOLDER_REGEX, service.getMetadata().getName())
                        .replaceAll(SERVICE_PORT_PLACEHOLDER_REGEX, Integer.toString(port))
                        .replaceAll(CONFIG_MAP_NAME_PLACEHOLDER_REGEX, containerServiceName)
                        .replaceAll(SECRET_MAP_NAME_PLACEHOLDER_REGEX, containerServiceName);
    }

    prepareIngress(kubernetesConfig, encryptedDataDetails, setupParams.isUseIngress(), ingressYaml,
        kubernetesServiceName, labels, executionLogCallback, summaryOutput);

    prepareRouteRule(encryptedDataDetails, kubernetesConfig, setupParams, kubernetesServiceName, service,
        activeServiceCounts, containerServiceName, executionLogCallback, summaryOutput);
  }

  private void setupServiceAndIngressForBlueGreen(KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, String revision,
      ExecutionLogCallback executionLogCallback, StringBuffer summaryOutput) {
    String primaryServiceName = getPrimaryServiceName(getKubernetesServiceName(setupParams.getControllerNamePrefix()));
    String stageServiceName = getStageServiceName(getKubernetesServiceName(setupParams.getControllerNamePrefix()));
    String ingressName = getBlueGreenIngressName(setupParams.getControllerNamePrefix());
    Map<String, String> labels = getLabels(setupParams);
    Map<String, String> selectors = setupParams.isUseNewLabelMechanism()
        ? getNewLabelSelectorsWithRevision(setupParams, revision)
        : getLabelSelectorsWithRevision(setupParams, revision);

    KubernetesServiceSpecification primaryServiceSpecification =
        (setupParams.isBlueGreen() && setupParams.getBlueGreenConfig() != null)
        ? setupParams.getBlueGreenConfig().getPrimaryService()
        : null;

    if (setupParams.isBlueGreen()) {
      executionLogCallback.saveExecutionLog(
          format("Setting Primary Service with name %s", primaryServiceName), LogLevel.INFO);
    }

    Service primaryService = prepareService(primaryServiceSpecification, kubernetesConfig, encryptedDataDetails,
        primaryServiceName, setupParams.getNamespace(), true, selectors, labels,
        setupParams.getServiceSteadyStateTimeout(), executionLogCallback, summaryOutput);

    KubernetesServiceSpecification stageServiceSpecification =
        (setupParams.isBlueGreen() && setupParams.getBlueGreenConfig() != null)
        ? setupParams.getBlueGreenConfig().getStageService()
        : null;

    if (setupParams.isBlueGreen()) {
      executionLogCallback.saveExecutionLog(
          format("Setting Stage Service with name %s", stageServiceName), LogLevel.INFO);
    }

    Service stageService = prepareService(stageServiceSpecification, kubernetesConfig, encryptedDataDetails,
        stageServiceName, setupParams.getNamespace(), false, selectors, labels,
        setupParams.getServiceSteadyStateTimeout(), executionLogCallback, summaryOutput);

    boolean useIngress = setupParams.isBlueGreen() && setupParams.getBlueGreenConfig() != null
        && setupParams.getBlueGreenConfig().isUseIngress();

    String ingressYaml = "";

    if (primaryService != null && stageService != null && useIngress) {
      int primaryServicePort =
          isNotEmpty(primaryService.getSpec().getPorts()) ? primaryService.getSpec().getPorts().get(0).getPort() : 80;

      int stageServicePort =
          isNotEmpty(stageService.getSpec().getPorts()) ? stageService.getSpec().getPorts().get(0).getPort() : 80;

      ingressYaml = setupParams.getBlueGreenConfig()
                        .getIngressYaml()
                        .replaceAll(PRIMARY_SERVICE_NAME_PLACEHOLDER_REGEX, primaryService.getMetadata().getName())
                        .replaceAll(PRIMARY_SERVICE_PORT_PLACEHOLDER_REGEX, Integer.toString(primaryServicePort))
                        .replaceAll(STAGE_SERVICE_NAME_PLACEHOLDER_REGEX, stageService.getMetadata().getName())
                        .replaceAll(STAGE_SERVICE_PORT_PLACEHOLDER_REGEX, Integer.toString(stageServicePort))
                        .replaceAll(CONFIG_MAP_NAME_PLACEHOLDER_REGEX, containerServiceName)
                        .replaceAll(SECRET_MAP_NAME_PLACEHOLDER_REGEX, containerServiceName);
    }

    prepareIngress(kubernetesConfig, encryptedDataDetails, useIngress, ingressYaml, ingressName, labels,
        executionLogCallback, summaryOutput);

    if (primaryService != null && stageService != null && setupParams.isBlueGreen()) {
      String primaryRevision = getRevisionFromService(primaryService);
      String stageRevision = getRevisionFromService(stageService);

      executionLogCallback.saveExecutionLog("Primary Service is at revision: " + primaryRevision);
      executionLogCallback.saveExecutionLog("Stage Service is at revision: " + stageRevision);
    }
  }

  private String getRevisionFromService(Service service) {
    String revision = service.getSpec().getSelector().get(HARNESS_REVISION);
    if (revision == null) {
      revision = service.getSpec().getSelector().get(HARNESS_KUBERNETES_REVISION_LABEL_KEY);
    }
    return revision;
  }

  private ImmutableMap<String, String> getLabels(KubernetesSetupParams setupParams) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_APP, KubernetesConvention.getLabelValue(setupParams.getAppName()))
        .put(HARNESS_SERVICE, KubernetesConvention.getLabelValue(setupParams.getServiceName()))
        .put(HARNESS_ENV, KubernetesConvention.getLabelValue(setupParams.getEnvName()))
        .put(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")
        .put(HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY,
            KubernetesConvention.getNormalizedInfraMappingIdLabelValue(setupParams.getInfraMappingId()))
        .build();
  }

  private ImmutableMap<String, String> getLabelSelectors(KubernetesSetupParams setupParams) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_APP, KubernetesConvention.getLabelValue(setupParams.getAppName()))
        .put(HARNESS_SERVICE, KubernetesConvention.getLabelValue(setupParams.getServiceName()))
        .put(HARNESS_ENV, KubernetesConvention.getLabelValue(setupParams.getEnvName()))
        .build();
  }

  private ImmutableMap<String, String> getNewLabelSelectors(KubernetesSetupParams setupParams) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY,
            KubernetesConvention.getNormalizedInfraMappingIdLabelValue(setupParams.getInfraMappingId()))
        .build();
  }

  private ImmutableMap<String, String> getLabelsWithRevision(KubernetesSetupParams setupParams, String revision) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_APP, KubernetesConvention.getLabelValue(setupParams.getAppName()))
        .put(HARNESS_SERVICE, KubernetesConvention.getLabelValue(setupParams.getServiceName()))
        .put(HARNESS_ENV, KubernetesConvention.getLabelValue(setupParams.getEnvName()))
        .put(HARNESS_REVISION, revision)
        .put(HARNESS_KUBERNETES_MANAGED_LABEL_KEY, "true")
        .put(HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY,
            KubernetesConvention.getNormalizedInfraMappingIdLabelValue(setupParams.getInfraMappingId()))
        .put(HARNESS_KUBERNETES_REVISION_LABEL_KEY, revision)
        .build();
  }

  private ImmutableMap<String, String> getHarnessAnnotations(KubernetesSetupParams setupParams) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_KUBERNETES_APP_LABEL_KEY, KubernetesConvention.getLabelValue(setupParams.getAppName()))
        .put(HARNESS_KUBERNETES_SERVICE_LABEL_KEY, KubernetesConvention.getLabelValue(setupParams.getServiceName()))
        .put(HARNESS_KUBERNETES_ENV_LABEL_KEY, KubernetesConvention.getLabelValue(setupParams.getEnvName()))
        .build();
  }

  private ImmutableMap<String, String> getLabelSelectorsWithRevision(
      KubernetesSetupParams setupParams, String revision) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_APP, KubernetesConvention.getLabelValue(setupParams.getAppName()))
        .put(HARNESS_SERVICE, KubernetesConvention.getLabelValue(setupParams.getServiceName()))
        .put(HARNESS_ENV, KubernetesConvention.getLabelValue(setupParams.getEnvName()))
        .put(HARNESS_REVISION, revision)
        .build();
  }

  private ImmutableMap<String, String> getNewLabelSelectorsWithRevision(
      KubernetesSetupParams setupParams, String revision) {
    return ImmutableMap.<String, String>builder()
        .put(HARNESS_KUBERNETES_INFRA_MAPPING_ID_LABEL_KEY,
            KubernetesConvention.getNormalizedInfraMappingIdLabelValue(setupParams.getInfraMappingId()))
        .put(HARNESS_KUBERNETES_REVISION_LABEL_KEY, revision)
        .build();
  }

  private Service prepareService(KubernetesServiceSpecification serviceSpecification, KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String kubernetesServiceName, String namespace,
      boolean keepExistingSelectors, Map<String, String> labelSelectors, Map<String, String> labels, int timeout,
      ExecutionLogCallback executionLogCallback, StringBuffer summaryOutput) {
    Service service = null;
    String serviceClusterIP;
    String serviceLoadBalancerEndpoint = null;
    String nodePort = null;

    try {
      service = kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
    }

    if (serviceSpecification == null || serviceSpecification.getServiceType() == null
        || serviceSpecification.getServiceType() == None) {
      if (service != null) {
        try {
          if (service.getSpec().getSelector().containsKey(HARNESS_APP)) {
            executionLogCallback.saveExecutionLog("Deleting existing service " + kubernetesServiceName);
            kubernetesContainerService.deleteService(kubernetesConfig, encryptedDataDetails, kubernetesServiceName);
          }
        } catch (Exception e) {
          logger.error("Couldn't delete service {}", kubernetesServiceName, e);
        }
      }
      return null;
    }

    if (service != null && keepExistingSelectors) {
      labelSelectors = service.getSpec().getSelector();
    }

    Service serviceToCreate = createServiceDefinition(
        kubernetesServiceName, namespace, labelSelectors, labels, serviceSpecification, executionLogCallback);
    if (service != null) {
      if (LOAD_BALANCER.equals(serviceToCreate.getSpec().getType())) {
        LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
        // Keep the previous load balancer IP if it exists and a new one was not specified
        if (isEmpty(serviceToCreate.getSpec().getLoadBalancerIP()) && loadBalancer != null
            && !loadBalancer.getIngress().isEmpty()) {
          serviceToCreate.getSpec().setLoadBalancerIP(loadBalancer.getIngress().get(0).getIp());
        }
        // When externalTrafficPolicy=Local the health check node port cannot change
        if (POLICY_LOCAL.equals(service.getSpec().getExternalTrafficPolicy())) {
          serviceToCreate.getSpec().setHealthCheckNodePort(service.getSpec().getHealthCheckNodePort());
        }
      }
    }

    service =
        kubernetesContainerService.createOrReplaceService(kubernetesConfig, encryptedDataDetails, serviceToCreate);
    serviceClusterIP = service.getSpec().getClusterIP();

    if (service.getSpec().getType().equals(LOAD_BALANCER)) {
      serviceLoadBalancerEndpoint = waitForLoadBalancerEndpoint(kubernetesConfig, encryptedDataDetails, service,
          serviceSpecification.getLoadBalancerIP(), timeout, executionLogCallback);
    } else if (service.getSpec().getType().equals(NODE_PORT)) {
      nodePort =
          Joiner.on(',').join(service.getSpec().getPorts().stream().map(ServicePort::getNodePort).collect(toList()));
    }

    summaryOutput.append(format("%nService Name: %s, ", kubernetesServiceName));

    if (isNotBlank(serviceClusterIP)) {
      summaryOutput.append(format("Cluster IP: %s ", serviceClusterIP));
    }
    if (isNotBlank(serviceLoadBalancerEndpoint)) {
      summaryOutput.append(format("Load Balancer Endpoint: %s ", serviceLoadBalancerEndpoint));
    }
    if (isNotBlank(nodePort)) {
      summaryOutput.append(format("Node Port: %s ", nodePort));
    }

    return service;
  }

  private HorizontalPodAutoscaler prepareHorizontalPodAutoscaler(KubernetesSetupParams setupParams, String name,
      String kind, String apiVersion, Map<String, String> controllerLabels, ExecutionLogCallback executionLogCallback) {
    if (setupParams.isUseAutoscaler()) {
      HorizontalPodAutoscaler autoscalerDefinition = createAutoscaler(
          name, kind, apiVersion, setupParams.getNamespace(), controllerLabels, setupParams, executionLogCallback);

      if (autoscalerDefinition != null) {
        executionLogCallback.saveExecutionLog(
            format("Autoscaler %s - disabled until 100%% deployed", name), LogLevel.INFO);
        return autoscalerDefinition;
      }
    }
    return null;
  }

  private IstioResource prepareRouteRule(List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesConfig kubernetesConfig, KubernetesSetupParams setupParams, String routeRuleName, Service service,
      Map<String, Integer> activeControllers, String containerServiceName, ExecutionLogCallback executionLogCallback,
      StringBuffer summaryOutput) {
    IstioResource routeRule = null;
    try {
      routeRule = kubernetesContainerService.getRouteRule(kubernetesConfig, encryptedDataDetails, routeRuleName);
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
    }

    if (setupParams.isUseIstioRouteRule() && service != null) {
      if (routeRule == null || routeRule.getSpec() == null || isEmpty(((RouteRule) routeRule.getSpec()).getRoute())
          || !((RouteRule) routeRule.getSpec()).getRoute().get(0).getLabels().containsKey(HARNESS_REVISION)) {
        IstioResource routeRuleDefinition = createRouteRuleDefinition(
            setupParams, routeRuleName, service.getSpec().getSelector(), activeControllers, executionLogCallback);
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

    if (routeRule != null) {
      summaryOutput.append(format("%nIstio route rule: %s", routeRule.getMetadata().getName()));
      printRouteRuleWeights(routeRule,
          getPrefixFromControllerName(containerServiceName, setupParams.isUseDashInHostname()),
          setupParams.isUseDashInHostname(), executionLogCallback);
    }

    return routeRule;
  }

  private Ingress prepareIngress(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      boolean useIngress, String ingressYaml, String ingressName, Map<String, String> labels,
      ExecutionLogCallback executionLogCallback, StringBuffer summaryOutput) {
    Ingress ingress;
    if (useIngress) {
      ingress = kubernetesContainerService.createOrReplaceIngress(kubernetesConfig, encryptedDataDetails,
          createIngressDefinition(ingressYaml, ingressName, labels, executionLogCallback));
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

    if (ingress != null) {
      summaryOutput.append(format("%nIngress Name: %s", ingress.getMetadata().getName()));
      printIngressRules(ingress, executionLogCallback, summaryOutput);
    }

    return ingress;
  }

  private Secret prepareSecretMap(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesSetupParams setupParams, String secretMapName, Map<String, String> controllerLabels,
      Map<String, String> safeDisplayServiceVariables, Map<String, String> serviceVariables,
      ExecutionLogCallback executionLogCallback) {
    Secret secretMap = new SecretBuilder()
                           .withNewMetadata()
                           .withAnnotations(harnessAnnotations)
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
        if (configMap == null) {
          throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
              .addParam("args", "Couldn't parse Config Map YAML: " + setupParams.getConfigMapYaml());
        }
        ObjectMeta configMapMeta = Optional.ofNullable(configMap.getMetadata()).orElse(new ObjectMeta());
        configMapMeta.setName(configMapName);
        configMapMeta.setNamespace(setupParams.getNamespace());
        configMapMeta.setAnnotations(mergeMaps(configMapMeta.getAnnotations(), harnessAnnotations));
        configMapMeta.setLabels(mergeMaps(configMapMeta.getLabels(), controllerLabels));
        configMap.setMetadata(configMapMeta);
        if (configMap.getData() == null) {
          configMap.setData(new HashMap<>());
        }
      } catch (Exception e) {
        throw new WingsException("Error while loading configMap yaml", e);
      }
    } else {
      configMap = new ConfigMapBuilder()
                      .withNewMetadata()
                      .withAnnotations(harnessAnnotations)
                      .withName(configMapName)
                      .withNamespace(setupParams.getNamespace())
                      .withLabels(controllerLabels)
                      .endMetadata()
                      .withData(new HashMap<>())
                      .build();
    }

    if (isNotEmpty(safeDisplayServiceVariables)) {
      configMap.getData().putAll(safeDisplayServiceVariables.entrySet()
                                     .stream()
                                     .filter(entry -> !SECRET_MASK.equals(entry.getValue()))
                                     .collect(toMap(Entry::getKey, Entry::getValue)));
    }

    if (isNotEmpty(setupParams.getPlainConfigFiles())) {
      configMap.getData().putAll(setupParams.getPlainConfigFiles().stream().collect(toMap(sa -> sa[0], sa -> sa[1])));
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

  private String getControllerYaml(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    HasMetadata controller =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, containerServiceName);
    if (controller != null) {
      try {
        return toYaml(controller);
      } catch (IOException e) {
        logger.error("Error converting controller to yaml: {}", containerServiceName);
      }
    }
    return null;
  }

  private String getConfigMapYaml(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    ConfigMap configMap =
        kubernetesContainerService.getConfigMap(kubernetesConfig, encryptedDataDetails, containerServiceName);
    if (configMap != null) {
      try {
        return toYaml(configMap);
      } catch (IOException e) {
        logger.error("Error converting configMap to yaml: {}", containerServiceName);
      }
    }
    return null;
  }

  private String getSecretMapYaml(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    Secret secretMap =
        kubernetesContainerService.getSecret(kubernetesConfig, encryptedDataDetails, containerServiceName);
    if (secretMap != null) {
      try {
        return toYaml(secretMap);
      } catch (IOException e) {
        logger.error("Error converting secretMap to yaml: {}", containerServiceName);
      }
    }
    return null;
  }

  private String getAutoscalerYaml(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName) {
    HorizontalPodAutoscaler hpa = kubernetesContainerService.getAutoscaler(
        kubernetesConfig, encryptedDataDetails, containerServiceName, KUBERNETES_V1.getVersionName());
    if (hpa != null) {
      try {
        return toYaml(hpa);
      } catch (IOException e) {
        logger.error("Error converting horizontal pod autoscaler to yaml: {}", containerServiceName);
      }
    }
    return null;
  }

  private Ingress createIngressDefinition(
      String ingressYaml, String ingressName, Map<String, String> labels, ExecutionLogCallback executionLogCallback) {
    try {
      Ingress ingress = KubernetesHelper.loadYaml(ingressYaml);
      if (ingress == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse Ingress YAML: " + ingressYaml);
      }
      ingress.getMetadata().setName(ingressName);
      ingress.getMetadata().setAnnotations(mergeMaps(ingress.getMetadata().getAnnotations(), harnessAnnotations));
      ingress.getMetadata().setLabels(mergeMaps(ingress.getMetadata().getLabels(), labels));
      executionLogCallback.saveExecutionLog("Setting ingress:\n\n" + toDisplayYaml(ingress));
      return ingress;
    } catch (IOException | RuntimeException e) {
      executionLogCallback.saveExecutionLog("Error reading Ingress from yaml: " + ingressName, LogLevel.ERROR);
    }
    return null;
  }

  private HorizontalPodAutoscaler createAutoscaler(String name, String kind, String apiVersion, String namespace,
      Map<String, String> serviceLabels, KubernetesSetupParams setupParams, ExecutionLogCallback executionLogCallback) {
    HorizontalPodAutoscaler horizontalPodAutoscaler;

    if (isNotBlank(setupParams.getCustomMetricYamlConfig())) {
      horizontalPodAutoscaler =
          getCustomMetricHorizontalPodAutoscaler(name, kind, apiVersion, namespace, serviceLabels, setupParams);
    } else {
      executionLogCallback.saveExecutionLog(
          format("Setting autoscaler min instances %d, max instances %d, with target CPU utilization %d%%",
              setupParams.getMinAutoscaleInstances(), setupParams.getMaxAutoscaleInstances(),
              setupParams.getTargetCpuUtilizationPercentage()),
          LogLevel.INFO);

      horizontalPodAutoscaler =
          getBasicHorizontalPodAutoscaler(name, kind, apiVersion, namespace, serviceLabels, setupParams);
    }

    executionLogCallback.saveExecutionLog(
        "Creating horizontal pod autoscaler:\n\n" + toDisplayYaml(horizontalPodAutoscaler));
    return horizontalPodAutoscaler;
  }

  private HorizontalPodAutoscaler getCustomMetricHorizontalPodAutoscaler(String name, String kind, String apiVersion,
      String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    try {
      HorizontalPodAutoscaler horizontalPodAutoscaler =
          KubernetesHelper.loadYaml(setupParams.getCustomMetricYamlConfig());

      if (horizontalPodAutoscaler == null) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam(
                "args", "Couldn't parse Horizontal Pod Autoscaler YAML: " + setupParams.getCustomMetricYamlConfig());
      }
      // set kind/name
      horizontalPodAutoscaler.getSpec().getScaleTargetRef().setName(name);
      horizontalPodAutoscaler.getSpec().getScaleTargetRef().setKind(kind);
      horizontalPodAutoscaler.getSpec().getScaleTargetRef().setApiVersion(apiVersion);

      // create metadata
      ObjectMeta objectMeta = horizontalPodAutoscaler.getMetadata();
      if (objectMeta == null) {
        objectMeta = new ObjectMeta();
        horizontalPodAutoscaler.setMetadata(objectMeta);
      }

      // set labels, name and namespace
      objectMeta.setAnnotations(mergeMaps(objectMeta.getAnnotations(), harnessAnnotations));
      objectMeta.setLabels(mergeMaps(objectMeta.getLabels(), serviceLabels));
      objectMeta.setName(name);
      objectMeta.setNamespace(namespace);

      return horizontalPodAutoscaler;
    } catch (Exception e) {
      throw new WingsException("Error while loading custom yaml for horizontal pod autoscaler", e);
    }
  }

  private HorizontalPodAutoscaler getBasicHorizontalPodAutoscaler(String name, String kind, String apiVersion,
      String namespace, Map<String, String> serviceLabels, KubernetesSetupParams setupParams) {
    HorizontalPodAutoscalerSpecBuilder spec =
        new HorizontalPodAutoscalerSpecBuilder()
            .withMinReplicas(setupParams.getMinAutoscaleInstances())
            .withMaxReplicas(setupParams.getMaxAutoscaleInstances())
            .withTargetCPUUtilizationPercentage(setupParams.getTargetCpuUtilizationPercentage())
            .withNewScaleTargetRef()
            .withKind(kind)
            .withName(name)
            .withApiVersion(apiVersion)
            .endScaleTargetRef();
    return new HorizontalPodAutoscalerBuilder()
        .withNewMetadata()
        .withAnnotations(harnessAnnotations)
        .withName(name)
        .withNamespace(namespace)
        .withLabels(serviceLabels)
        .endMetadata()
        .withSpec(spec.build())
        .build();
  }

  private IstioResource createRouteRuleDefinition(KubernetesSetupParams setupParams, String kubernetesServiceName,
      Map<String, String> serviceLabels, Map<String, Integer> activeControllers,
      ExecutionLogCallback executionLogCallback) {
    RouteRuleSpecNested<IstioResourceBuilder> routeRuleSpecNested = new IstioResourceBuilder()
                                                                        .withNewMetadata()
                                                                        .withAnnotations(harnessAnnotations)
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
    for (Entry<String, Integer> entry : activeControllers.entrySet()) {
      Optional<Integer> revision = getRevisionFromControllerName(entry.getKey(), setupParams.isUseDashInHostname());
      if (revision.isPresent()) {
        int weight = (int) Math.round((entry.getValue() * 100.0) / totalInstances);
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

  private void listContainerInfosWhenReady(List<EncryptedDataDetail> encryptedDataDetails,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback, KubernetesConfig kubernetesConfig,
      String containerServiceName, List<Pod> originalPods, long startTime, boolean isRollback) {
    Optional<Integer> controllerPodCount =
        kubernetesContainerService.getControllerPodCount(kubernetesConfig, encryptedDataDetails, containerServiceName);
    int desiredCount = controllerPodCount.isPresent() ? controllerPodCount.get() : 0;
    int previousCount =
        kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, containerServiceName) != null
        ? desiredCount
        : 0;
    List<ContainerInfo> containerInfos = kubernetesContainerService.getContainerInfosWhenReady(kubernetesConfig,
        encryptedDataDetails, containerServiceName, previousCount, serviceSteadyStateTimeout, originalPods, true,
        executionLogCallback, true, startTime);

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
          format("Completed operation with errors%n%s%n", DASH_STRING), LogLevel.ERROR);
      throw new WingsException(ErrorCode.DEFAULT_ERROR_CODE).addParam("message", "Pods failed to reach desired count");
    }
    executionLogCallback.saveExecutionLog("\nContainer IDs:");
    containerInfos.forEach(info
        -> executionLogCallback.saveExecutionLog(
            "  " + info.getHostName() + " - " + info.getIp() + " - " + info.getContainerId()));
    executionLogCallback.saveExecutionLog("");
    if (!isRollback) {
      executionLogCallback.saveExecutionLog(format("Completed operation%n%s%n", DASH_STRING));
    }
  }

  private void performYamlRollback(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesSetupParams setupParams, KubernetesConfig kubernetesConfig,
      String controllerName) {
    String controllerYaml = null;
    String configMapYaml = null;
    String secretMapYaml = null;
    String autoscalerYaml = null;

    if (setupParams.getPreviousYamlConfig() != null) {
      controllerYaml = setupParams.getPreviousYamlConfig().getControllerYaml();
      configMapYaml = setupParams.getPreviousYamlConfig().getConfigMapYaml();
      secretMapYaml = setupParams.getPreviousYamlConfig().getSecretMapYaml();
      autoscalerYaml = setupParams.getPreviousYamlConfig().getAutoscalerYaml();
    }

    if (isNotBlank(configMapYaml)) {
      executionLogCallback.saveExecutionLog("Rolling back configMap: " + controllerName);
      ConfigMap configMap;
      try {
        configMap = KubernetesHelper.loadYaml(configMapYaml);
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse configMap YAML: " + configMapYaml);
      }
      executionLogCallback.saveExecutionLog("Setting configMap:\n\n" + toDisplayYaml(configMap));
      kubernetesContainerService.createOrReplaceConfigMap(kubernetesConfig, encryptedDataDetails, configMap);
    } else {
      ConfigMap configMap =
          kubernetesContainerService.getConfigMap(kubernetesConfig, encryptedDataDetails, controllerName);
      if (configMap != null) {
        executionLogCallback.saveExecutionLog(
            "ConfigMap " + controllerName + " did not exist previously. Deleting on rollback");
        try {
          kubernetesContainerService.deleteConfigMap(kubernetesConfig, encryptedDataDetails, controllerName);
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Error deleting configMap: " + controllerName, LogLevel.ERROR);
          Misc.logAllMessages(e, executionLogCallback);
        }
      }
    }

    if (isNotBlank(secretMapYaml)) {
      executionLogCallback.saveExecutionLog("Rolling back secretMap: " + controllerName);
      Secret secretMap;
      try {
        secretMap = KubernetesHelper.loadYaml(secretMapYaml);
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse secretMap YAML: " + controllerName);
      }
      executionLogCallback.saveExecutionLog("Setting secretMap:\n\n"
          + toDisplayYaml(new SecretBuilder()
                              .withMetadata(secretMap.getMetadata())
                              .withStringData(secretMap.getData().entrySet().stream().collect(
                                  toMap(Entry::getKey, entry -> SECRET_MASK)))
                              .build()));
      kubernetesContainerService.createOrReplaceSecret(kubernetesConfig, encryptedDataDetails, secretMap);
    } else {
      Secret secretMap = kubernetesContainerService.getSecret(kubernetesConfig, encryptedDataDetails, controllerName);
      if (secretMap != null) {
        executionLogCallback.saveExecutionLog(
            "SecretMap " + controllerName + " did not exist previously. Deleting on rollback");
        try {
          kubernetesContainerService.deleteSecret(kubernetesConfig, encryptedDataDetails, controllerName);
        } catch (Exception e) {
          executionLogCallback.saveExecutionLog("Error deleting secretMap: " + controllerName, LogLevel.ERROR);
          Misc.logAllMessages(e, executionLogCallback);
        }
      }
    }

    long startTime = clock.millis();
    List<Pod> originalPods =
        kubernetesContainerService.getRunningPods(kubernetesConfig, encryptedDataDetails, controllerName);
    if (isNotBlank(controllerYaml)) {
      HasMetadata controller;
      try {
        controller = KubernetesHelper.loadYaml(controllerYaml);
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Couldn't parse controller YAML: " + controllerYaml);
      }
      executionLogCallback.saveExecutionLog("Rolling back controller " + controllerName);
      executionLogCallback.saveExecutionLog("Setting controller:\n\n" + toDisplayYaml(controller));
      kubernetesContainerService.createOrReplaceController(kubernetesConfig, encryptedDataDetails, controller);
      PodTemplateSpec podTemplateSpec = kubernetesContainerService.getPodTemplateSpec(controller);
      executionLogCallback.saveExecutionLog("Rolled back to controller with image: "
              + podTemplateSpec.getSpec().getContainers().stream().map(Container::getImage).collect(toList()),
          LogLevel.INFO);
      listContainerInfosWhenReady(encryptedDataDetails, setupParams.getServiceSteadyStateTimeout(),
          executionLogCallback, kubernetesConfig, controllerName, originalPods, startTime, true);
    } else {
      executionLogCallback.saveExecutionLog(
          "Controller " + controllerName + " did not exist previously. Deleting on rollback");
      try {
        HasMetadata controller =
            kubernetesContainerService.getController(kubernetesConfig, encryptedDataDetails, controllerName);
        Map<String, String> labels = controller.getMetadata().getLabels();
        kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, controllerName);
        kubernetesContainerService.waitForPodsToStop(kubernetesConfig, encryptedDataDetails, labels,
            setupParams.getServiceSteadyStateTimeout(), originalPods, startTime, executionLogCallback);
      } catch (Exception e) {
        executionLogCallback.saveExecutionLog("Error deleting controller: " + controllerName, LogLevel.ERROR);
        Misc.logAllMessages(e, executionLogCallback);
      }
    }

    rollbackAutoscaler(encryptedDataDetails, executionLogCallback, kubernetesConfig, autoscalerYaml);
  }

  private void performAutoscalerRollback(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback,
      KubernetesSetupParams setupParams) {
    String autoscalerYaml = setupParams.getPreviousYamlConfig().getAutoscalerYaml();
    if (isNotBlank(autoscalerYaml)) {
      rollbackAutoscaler(encryptedDataDetails, executionLogCallback, kubernetesConfig, autoscalerYaml);
    }
  }

  private void rollbackAutoscaler(List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback, KubernetesConfig kubernetesConfig, String autoscalerYaml) {
    HorizontalPodAutoscaler hpa =
        kubernetesContainerService.createOrReplaceAutoscaler(kubernetesConfig, encryptedDataDetails, autoscalerYaml);
    if (hpa != null) {
      String hpaName = hpa.getMetadata().getName();
      executionLogCallback.saveExecutionLog("Rolled back horizontal pod autoscaler " + hpaName);
      executionLogCallback.saveExecutionLog("Set horizontal pod autoscaler:\n\n" + toDisplayYaml(hpa));
    }
  }

  private String getApiVersionForHPA(String yamlConfig) {
    return isBlank(yamlConfig) ? ContainerApiVersions.KUBERNETES_V1.getVersionName()
                               : ContainerApiVersions.KUBERNETES_V2_BETA1.getVersionName();
  }

  @SuppressFBWarnings("DM_DEFAULT_ENCODING")
  private Secret createRegistrySecret(String secretName, String namespace, ImageDetails imageDetails,
      Map<String, String> controllerLabels, ExecutionLogCallback executionLogCallback) {
    executionLogCallback.saveExecutionLog("Setting image pull secret " + secretName);
    String credentialData = format(DOCKER_REGISTRY_CREDENTIAL_TEMPLATE, imageDetails.getRegistryUrl(),
        imageDetails.getUsername(), imageDetails.getPassword());
    Map<String, String> data =
        ImmutableMap.of(".dockercfg", new String(Base64.getEncoder().encode(credentialData.getBytes())));
    return new SecretBuilder()
        .withNewMetadata()
        .withAnnotations(harnessAnnotations)
        .withLabels(controllerLabels)
        .withName(secretName)
        .withNamespace(namespace)
        .endMetadata()
        .withType("kubernetes.io/dockercfg")
        .withData(data)
        .build();
  }

  private String waitForLoadBalancerEndpoint(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, Service service, String loadBalancerIP, int timeoutInMinutes,
      ExecutionLogCallback executionLogCallback) {
    String serviceName = service.getMetadata().getName();
    LoadBalancerStatus loadBalancer = service.getStatus().getLoadBalancer();
    if (loadBalancer != null
        && (loadBalancer.getIngress().isEmpty()
               || (isNotBlank(loadBalancerIP) && !loadBalancerIP.equals(loadBalancer.getIngress().get(0).getIp())))) {
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
        }, timeoutInMinutes, TimeUnit.MINUTES, true);
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
        isNotBlank(loadBalancerIngress.getHostname()) ? loadBalancerIngress.getHostname() : loadBalancerIngress.getIp();
    executionLogCallback.saveExecutionLog(
        format("Service [%s] load balancer is ready with endpoint [%s]", serviceName, loadBalancerEndpoint),
        LogLevel.INFO);
    return loadBalancerEndpoint;
  }

  private String lastController(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String controllerNamePrefix, boolean useDashInHostName) {
    final AtomicReference<HasMetadata> lastController = new AtomicReference<>();
    final AtomicInteger lastRevision = new AtomicInteger();
    kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
        .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
        .forEach(ctrl -> {
          Optional<Integer> revision = getRevisionFromControllerName(ctrl.getMetadata().getName(), useDashInHostName);
          if (revision.isPresent() && (lastController.get() == null || revision.get() > lastRevision.get())) {
            lastController.set(ctrl);
            lastRevision.set(revision.get());
          }
        });
    return lastController.get() != null ? lastController.get().getMetadata().getName() : null;
  }

  private HasMetadata getLastDeployment(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, KubernetesSetupParams setupParams) {
    final AtomicReference<HasMetadata> lastDeployment = new AtomicReference<>();
    final AtomicInteger lastRevision = new AtomicInteger(-1);
    kubernetesContainerService.getControllers(kubernetesConfig, encryptedDataDetails, lookupLabels)
        .forEach(deployment -> {
          int revision = getRevisionNumber(deployment);
          if (revision > lastRevision.get()) {
            lastDeployment.set(deployment);
            lastRevision.set(revision);
          }
        });
    return lastDeployment.get() != null ? lastDeployment.get() : null;
  }

  private int getRevisionNumber(HasMetadata kubernetesResource) {
    return isNotVersioned || kubernetesResource == null
        ? 0
        : Integer.parseInt(kubernetesResource.getMetadata().getLabels().get(HARNESS_KUBERNETES_REVISION_LABEL_KEY));
  }

  private String lastAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String controllerNamePrefix, boolean useDashInHostName) {
    final AtomicReference<HorizontalPodAutoscaler> lastHpa = new AtomicReference<>();
    final AtomicInteger lastRevision = new AtomicInteger();
    kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
        .stream()
        .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
        .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
        .forEach(ctrl -> {
          HorizontalPodAutoscaler hpa = kubernetesContainerService.getAutoscaler(
              kubernetesConfig, encryptedDataDetails, ctrl.getMetadata().getName(), KUBERNETES_V1.getVersionName());
          Optional<Integer> revision = getRevisionFromControllerName(ctrl.getMetadata().getName(), useDashInHostName);
          if (hpa != null && revision.isPresent() && (lastHpa.get() == null || revision.get() > lastRevision.get())) {
            lastHpa.set(hpa);
            lastRevision.set(revision.get());
          }
        });
    return lastHpa.get() != null ? lastHpa.get().getMetadata().getName() : null;
  }

  private HasMetadata createKubernetesControllerDefinition(KubernetesContainerTask kubernetesContainerTask,
      String controllerName, Map<String, String> controllerLabels, KubernetesSetupParams setupParams,
      String registrySecretName, ConfigMap configMap, Secret secretMap, List<Pod> originalPods,
      ExecutionLogCallback executionLogCallback) {
    String containerName = KubernetesConvention.getContainerName(setupParams.getImageDetails().getName());
    String imageNameTag = setupParams.getImageDetails().getName() + ":" + setupParams.getImageDetails().getTag();
    String domainName = setupParams.getImageDetails().getDomainName();

    String configMapName = configMap != null ? configMap.getMetadata().getName() : "no-config-map";
    String secretMapName = secretMap != null ? secretMap.getMetadata().getName() : "no-secret-map";

    HasMetadata controller = kubernetesContainerTask.createController(
        containerName, imageNameTag, registrySecretName, configMapName, secretMapName, domainName);

    KubernetesHelper.setName(controller, controllerName);
    KubernetesHelper.setNamespace(controller, setupParams.getNamespace());
    KubernetesHelper.getOrCreateLabels(controller).putAll(controllerLabels);
    controller.getMetadata().setAnnotations(mergeMaps(controller.getMetadata().getAnnotations(), harnessAnnotations));

    configureTypeSpecificSpecs(
        controllerLabels, controller, configMap, secretMap, setupParams, originalPods, executionLogCallback);
    return controller;
  }

  private void configureTypeSpecificSpecs(Map<String, String> controllerLabels, HasMetadata kubernetesObj,
      ConfigMap configMap, Secret secretMap, KubernetesSetupParams setupParams, List<Pod> originalPods,
      ExecutionLogCallback executionLogCallback) {
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
      int maxInstances = setupParams.getMaxInstances();
      int totalActiveServiceCount = Optional.ofNullable(originalPods).orElse(emptyList()).size();
      if (totalActiveServiceCount > 0) {
        maxInstances = totalActiveServiceCount;
      }
      stateSetSpec.setReplicas(setupParams.isUseFixedInstances() ? setupParams.getFixedInstances() : maxInstances);
      podTemplateSpec = stateSetSpec.getTemplate();
    }

    Preconditions.checkNotNull(podTemplateSpec, "Pod template spec is missing in controller definition");
    Preconditions.checkNotNull(
        podTemplateSpec.getSpec(), "Pod spec in pod template spec is missing in controller definition");

    if (podTemplateSpec.getMetadata() == null) {
      podTemplateSpec.setMetadata(new ObjectMeta());
    }

    podTemplateSpec.getMetadata().setAnnotations(
        mergeMaps(podTemplateSpec.getMetadata().getAnnotations(), harnessAnnotations));
    podTemplateSpec.getMetadata().setLabels(mergeMaps(podTemplateSpec.getMetadata().getLabels(), controllerLabels));

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

  private Map<String, String> mergeMaps(Map<String, String> map1, Map<String, String> map2) {
    Map<String, String> newMap = new HashMap<>();
    if (map1 != null) {
      newMap.putAll(map1);
    }
    if (map2 != null) {
      newMap.putAll(map2);
    }
    return newMap;
  }

  private io.fabric8.kubernetes.api.model.Service createServiceDefinition(String serviceName, String namespace,
      Map<String, String> labelSelectors, Map<String, String> labels,
      KubernetesServiceSpecification serviceSpecification, ExecutionLogCallback executionLogCallback) {
    if (serviceSpecification.getServiceType() == KubernetesServiceType.Yaml) {
      try {
        Service service = KubernetesHelper.loadYaml(serviceSpecification.getServiceYaml());
        if (service == null) {
          throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
              .addParam("args", "Couldn't parse Service YAML: " + serviceSpecification.getServiceYaml());
        }
        service.getMetadata().setAnnotations(mergeMaps(service.getMetadata().getAnnotations(), harnessAnnotations));
        service.getMetadata().setLabels(mergeMaps(service.getMetadata().getLabels(), labels));
        service.getMetadata().setName(serviceName);
        service.getMetadata().setNamespace(namespace);
        service.getSpec().setSelector(mergeMaps(service.getSpec().getSelector(), labelSelectors));
        executionLogCallback.saveExecutionLog("Setting service:\n\n" + toDisplayYaml(service));
        return service;
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, e).addParam("args", Misc.getMessage(e));
      }
    } else {
      ServiceSpecBuilder spec =
          new ServiceSpecBuilder().withSelector(labelSelectors).withType(serviceSpecification.getServiceType().name());

      if (serviceSpecification.getServiceType() != KubernetesServiceType.ExternalName) {
        ServicePortBuilder servicePort =
            new ServicePortBuilder()
                .withProtocol(serviceSpecification.getProtocol().name())
                .withPort(serviceSpecification.getPort())
                .withNewTargetPort()
                .withIntVal(serviceSpecification.getTargetPort())
                .endTargetPort()
                .withName(isNotBlank(serviceSpecification.getPortName()) ? serviceSpecification.getPortName() : "http");
        if (serviceSpecification.getServiceType() == KubernetesServiceType.NodePort
            && serviceSpecification.getNodePort() != null) {
          servicePort.withNodePort(serviceSpecification.getNodePort());
        }
        spec.withPorts(ImmutableList.of(servicePort.build()));

        if (serviceSpecification.getServiceType() == KubernetesServiceType.LoadBalancer
            && isNotBlank(serviceSpecification.getLoadBalancerIP())) {
          spec.withLoadBalancerIP(serviceSpecification.getLoadBalancerIP());
        }

        if (serviceSpecification.getServiceType() == KubernetesServiceType.ClusterIP
            && isNotBlank(serviceSpecification.getClusterIP())) {
          spec.withClusterIP(serviceSpecification.getClusterIP());
        }
      } else {
        spec.withExternalName(serviceSpecification.getExternalName());
      }

      if (isNotBlank(serviceSpecification.getExternalIPs())) {
        spec.withExternalIPs(
            Arrays.stream(serviceSpecification.getExternalIPs().split(",")).map(String::trim).collect(toList()));
      }

      Service service = new ServiceBuilder()
                            .withNewMetadata()
                            .withAnnotations(harnessAnnotations)
                            .withName(serviceName)
                            .withNamespace(namespace)
                            .withLabels(labels)
                            .endMetadata()
                            .withSpec(spec.build())
                            .build();
      executionLogCallback.saveExecutionLog("Setting service:\n\n" + toDisplayYaml(service));
      return service;
    }
  }

  private void downsizeOldOrUnhealthy(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String containerServiceName, KubernetesSetupParams setupParams, ExecutionLogCallback executionLogCallback,
      boolean useDashInHostname) {
    Map<String, Integer> activeCounts;

    if (setupParams.isUseNewLabelMechanism()) {
      activeCounts = kubernetesContainerService.getActiveServiceCountsWithLabels(
          kubernetesConfig, encryptedDataDetails, lookupLabels);
    } else {
      activeCounts = kubernetesContainerService.getActiveServiceCounts(
          kubernetesConfig, encryptedDataDetails, containerServiceName, useDashInHostname);
    }

    String latestHealthyController = null;
    if (activeCounts.size() > 1) {
      executionLogCallback.saveExecutionLog("\nActive pods:");
      for (Entry<String, Integer> entry : activeCounts.entrySet()) {
        String activeControllerName = entry.getKey();
        List<Pod> pods =
            kubernetesContainerService.getRunningPods(kubernetesConfig, encryptedDataDetails, activeControllerName);
        List<ContainerInfo> containerInfos =
            kubernetesContainerService.getContainerInfosWhenReady(kubernetesConfig, encryptedDataDetails,
                activeControllerName, entry.getValue(), 1, pods, false, executionLogCallback, false, clock.millis());
        boolean allContainersSuccess =
            containerInfos.stream().allMatch(info -> info.getStatus() == ContainerInfo.Status.SUCCESS);
        if (allContainersSuccess) {
          latestHealthyController = activeControllerName;
        }
      }

      for (Entry<String, Integer> entry : activeCounts.entrySet()) {
        String controllerName = entry.getKey();
        if (!controllerName.equals(latestHealthyController)) {
          executionLogCallback.saveExecutionLog("");
          kubernetesContainerService.setControllerPodCount(kubernetesConfig, encryptedDataDetails,
              setupParams.getClusterName(), controllerName, entry.getValue(), 0,
              setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
        }
      }
    }
  }

  private void cleanup(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String containerServiceName, ExecutionLogCallback executionLogCallback, boolean useDashInHostname) {
    executionLogCallback.saveExecutionLog("\nRemoving versions with no pods");
    Optional<Integer> revision = getRevisionFromControllerName(containerServiceName, useDashInHostname);
    if (revision.isPresent()) {
      String controllerNamePrefix = getPrefixFromControllerName(containerServiceName, useDashInHostname);
      kubernetesContainerService.listControllers(kubernetesConfig, encryptedDataDetails)
          .stream()
          .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
          .filter(ctrl -> !ctrl.getMetadata().getName().equals(containerServiceName))
          .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
          .filter(ctrl -> kubernetesContainerService.getControllerPodCount(ctrl) == 0)
          .forEach(ctrl -> {
            String controllerName = ctrl.getMetadata().getName();
            Optional<Integer> ctrlRevision = getRevisionFromControllerName(controllerName, useDashInHostname);
            if (ctrlRevision.isPresent()) {
              logger.info("Deleting old version: " + controllerName);
              executionLogCallback.saveExecutionLog("Deleting old version: " + controllerName);
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

  private void cleanupWithLabels(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback) {
    if (isNotVersioned) {
      return;
    }
    executionLogCallback.saveExecutionLog("\nRemoving versions with no pods");

    kubernetesContainerService.getControllers(kubernetesConfig, encryptedDataDetails, lookupLabels)
        .stream()
        .filter(ctrl
            -> kubernetesContainerService.getControllerPodCount(ctrl) == 0
                && getRevisionNumber(ctrl) != currentRevision)
        .filter(ctrl -> !(ctrl.getKind().equals("ReplicaSet") && ctrl.getMetadata().getOwnerReferences() != null))
        .forEach(ctrl -> {
          String controllerName = ctrl.getMetadata().getName();
          logger.info("Deleting old version: " + controllerName);
          executionLogCallback.saveExecutionLog("Deleting old version: " + controllerName);
          try {
            kubernetesContainerService.deleteController(kubernetesConfig, encryptedDataDetails, controllerName);
            kubernetesContainerService.deleteAutoscaler(kubernetesConfig, encryptedDataDetails, controllerName);
            kubernetesContainerService.deleteConfigMap(kubernetesConfig, encryptedDataDetails, controllerName);
            kubernetesContainerService.deleteSecret(kubernetesConfig, encryptedDataDetails, controllerName);
          } catch (Exception e) {
            Misc.logAllMessages(e, executionLogCallback);
          }
        });

    kubernetesContainerService.getServices(kubernetesConfig, encryptedDataDetails, lookupLabels)
        .stream()
        .filter(service -> !(service.getMetadata().getName().equals(getKubernetesServiceName(currentNamePrefix))))
        .filter(service
            -> !(service.getMetadata().getName().equals(
                getKubernetesServiceName(getPrimaryServiceName(currentNamePrefix)))))
        .filter(service
            -> !(service.getMetadata().getName().equals(
                getKubernetesServiceName(getStageServiceName(currentNamePrefix)))))
        .filter(service
            -> kubernetesContainerService
                    .getPods(kubernetesConfig, encryptedDataDetails, service.getSpec().getSelector())
                    .size()
                == 0)
        .forEach(service -> {
          String serviceName = service.getMetadata().getName();
          logger.info("Deleting old service: " + serviceName);
          executionLogCallback.saveExecutionLog("Deleting old service: " + serviceName);
          try {
            kubernetesContainerService.deleteService(kubernetesConfig, encryptedDataDetails, serviceName);
            kubernetesContainerService.deleteIngress(kubernetesConfig, encryptedDataDetails, serviceName);
          } catch (Exception e) {
            Misc.logAllMessages(e, executionLogCallback);
          }
        });
  }

  private void cleanupStageDeployment(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      KubernetesSetupParams setupParams, ExecutionLogCallback executionLogCallback, boolean useDashInHostname) {
    String primaryServiceName = getPrimaryServiceName(getKubernetesServiceName(setupParams.getControllerNamePrefix()));
    String stageServiceName = getStageServiceName(getKubernetesServiceName(setupParams.getControllerNamePrefix()));

    Service primaryService;
    Service stageService;

    try {
      primaryService =
          kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, primaryServiceName);
      stageService = kubernetesContainerService.getService(kubernetesConfig, encryptedDataDetails, stageServiceName);
    } catch (Exception e) {
      Misc.logAllMessages(e, executionLogCallback);
      return;
    }

    if (stageService == null) {
      executionLogCallback.saveExecutionLog("No Stage Service found.");
      return;
    }

    String primaryRevision = getRevisionFromService(primaryService);
    String stageRevision = getRevisionFromService(stageService);

    executionLogCallback.saveExecutionLog("Primary Service is at revision: " + primaryRevision);
    executionLogCallback.saveExecutionLog("Stage Service is at revision: " + stageRevision);

    if (!StringUtils.equals(primaryRevision, stageRevision)) {
      executionLogCallback.saveExecutionLog("\nScaling down Stage Deployment [Revision: " + stageRevision + "] to 0");
      String controllerName = KubernetesConvention.getControllerName(
          setupParams.getControllerNamePrefix(), Integer.parseInt(stageRevision), useDashInHostname);

      Optional<Integer> podCount =
          kubernetesContainerService.getControllerPodCount(kubernetesConfig, encryptedDataDetails, controllerName);

      if (podCount.isPresent() && podCount.get() > 0) {
        kubernetesContainerService.setControllerPodCount(kubernetesConfig, encryptedDataDetails,
            setupParams.getClusterName(), controllerName, podCount.orElse(1), 0,
            setupParams.getServiceSteadyStateTimeout(), executionLogCallback);
      } else {
        executionLogCallback.saveExecutionLog("No Pods found for Stage Deployment. Continue..");
      }
    } else {
      executionLogCallback.saveExecutionLog("Skipping Cleanup as Primary and Stage are using same Deployment");
    }
  }

  private void printIngressRules(
      Ingress ingress, ExecutionLogCallback executionLogCallback, StringBuffer summaryOutput) {
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
        summaryOutput.append(
            format("%nIngress Rule: %s : %s %s → %s", isNotBlank(host) ? host : "", port, path, serviceName));
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
