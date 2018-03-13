package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.yaml.YamlHelper.trimYaml;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ConfigFile;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.service.intfc.ConfigService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.KubernetesConvention;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 3/1/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesSetup extends ContainerServiceSetup {
  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/KubernetesRepCtrlSetup.js

  @Transient @Inject private transient ConfigService configService;

  private String replicationControllerName;
  private KubernetesServiceType serviceType;
  private Integer port;
  private Integer targetPort;
  private String portName;
  private KubernetesPortProtocol protocol;
  private String clusterIP;
  private String externalIPs;
  private String loadBalancerIP;
  private Integer nodePort;
  private String externalName;
  private boolean useAutoscaler;
  private int minAutoscaleInstances;
  private int maxAutoscaleInstances;
  private int targetCpuUtilizationPercentage;
  private boolean useIngress;
  private String ingressYaml;
  private String customMetricYamlConfig;
  private boolean useIstioRouteRule;

  private String commandName = "Setup Replication Controller";

  public KubernetesSetup(String name) {
    super(name, KUBERNETES_SETUP.name());
  }

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, Service service,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask, String clusterName) {
    Map<String, String> configFiles = new HashMap<>();

    Map<String, String> configFilesService = getConfigFileContent(app, service.getConfigFiles());
    if (isNotEmpty(configFilesService)) {
      configFiles.putAll(configFilesService);
    }

    Map<String, String> configFilesFromEnv =
        getConfigFileContent(app, configService.getConfigFileOverridesForEnv(app.getUuid(), env.getUuid()));
    if (isNotEmpty(configFilesFromEnv)) {
      configFiles.putAll(configFilesFromEnv);
    }

    String configMapYaml = null;
    // TODO(brett)
    //    String configMapYaml = service.getConfigMapYaml();
    //    if (isNotBlank(env.getConfigMapYaml())) {
    //      configMapYaml = env.getConfigMapYaml();
    //    }

    String configMapYamlEvaluated = null;
    if (isNotBlank(configMapYaml)) {
      configMapYamlEvaluated = context.renderExpression(configMapYaml);
    }

    String controllerNamePrefix = isNotBlank(replicationControllerName)
        ? KubernetesConvention.normalize(context.renderExpression(replicationControllerName))
        : KubernetesConvention.getControllerNamePrefix(app.getName(), serviceName, env.getName());

    if (containerTask != null) {
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      kubernetesContainerTask.getContainerDefinitions()
          .stream()
          .filter(containerDefinition -> isNotEmpty(containerDefinition.getCommands()))
          .forEach(containerDefinition
              -> containerDefinition.setCommands(
                  containerDefinition.getCommands().stream().map(context::renderExpression).collect(toList())));
      if (kubernetesContainerTask.getAdvancedConfig() != null) {
        kubernetesContainerTask.setAdvancedConfig(
            context.renderExpression(kubernetesContainerTask.getAdvancedConfig()));
      }
    }

    String ingressYamlEvaluated = null;
    if (isNotBlank(ingressYaml)) {
      ingressYamlEvaluated = context.renderExpression(ingressYaml);
    }

    String customMetricYamlEvaluated = null;
    if (isNotBlank(customMetricYamlConfig)) {
      customMetricYamlEvaluated = context.renderExpression(customMetricYamlConfig);
    }

    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;

    String subscriptionId = null;
    String resourceGroup = null;

    if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
      subscriptionId = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getSubscriptionId();
      resourceGroup = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getResourceGroup();
    }

    String namespace = null;
    if (infrastructureMapping instanceof GcpKubernetesInfrastructureMapping) {
      namespace = ((GcpKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
    } else if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
      namespace = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
    } else if (infrastructureMapping instanceof DirectKubernetesInfrastructureMapping) {
      namespace = ((DirectKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
    } else {
      unhandled(infrastructureMapping.getInfraMappingType());
    }
    if (isBlank(namespace)) {
      namespace = "default";
    }

    return aKubernetesSetupParams()
        .withAppName(app.getName())
        .withEnvName(env.getName())
        .withServiceName(serviceName)
        .withClusterName(clusterName)
        .withNamespace(namespace)
        .withImageDetails(imageDetails)
        .withClusterIP(clusterIP)
        .withContainerTask(containerTask)
        .withExternalIPs(externalIPs)
        .withExternalName(externalName)
        .withInfraMappingId(infrastructureMapping.getUuid())
        .withLoadBalancerIP(loadBalancerIP)
        .withNodePort(nodePort)
        .withPort(port)
        .withProtocol(protocol)
        .withServiceType(serviceType)
        .withTargetPort(targetPort)
        .withPortName(portName)
        .withControllerNamePrefix(controllerNamePrefix)
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .withUseAutoscaler(useAutoscaler)
        .withMinAutoscaleInstances(minAutoscaleInstances)
        .withMaxAutoscaleInstances(maxAutoscaleInstances)
        .withTargetCpuUtilizationPercentage(targetCpuUtilizationPercentage)
        .withCustomMetricYamlConfig(customMetricYamlEvaluated)
        .withSubscriptionId(subscriptionId)
        .withResourceGroup(resourceGroup)
        .withUseIngress(useIngress)
        .withIngressYaml(ingressYamlEvaluated)
        .withUseIstioRouteRule(useIstioRouteRule)
        .withConfigFiles(configFiles)
        .withConfigMapYaml(configMapYamlEvaluated)
        .build();
  }

  private Map<String, String> getConfigFileContent(Application app, List<ConfigFile> configFiles) {
    return configFiles.stream().collect(toMap(cf
        -> cf.getFileName().replaceAll("\\.", "_"),
        cf -> configService.getFileContent(app.getUuid(), cf.getUuid())));
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(
      ExecutionContext context, CommandExecutionResult executionResult, ExecutionStatus status) {
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    KubernetesSetupParams setupParams = (KubernetesSetupParams) executionData.getContainerSetupParams();
    int evaluatedMaxInstances =
        isNotBlank(getMaxInstances()) ? Integer.valueOf(context.renderExpression(getMaxInstances())) : DEFAULT_MAX;
    int maxInstances = evaluatedMaxInstances == 0 ? DEFAULT_MAX : evaluatedMaxInstances;
    int evaluatedFixedInstances =
        isNotBlank(getFixedInstances()) ? Integer.valueOf(context.renderExpression(getFixedInstances())) : maxInstances;
    int fixedInstances = evaluatedFixedInstances == 0 ? maxInstances : evaluatedFixedInstances;
    ResizeStrategy resizeStrategy = getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy();
    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;

    ContainerServiceElementBuilder containerServiceElementBuilder =
        ContainerServiceElement.builder()
            .uuid(executionData.getServiceId())
            .useFixedInstances(FIXED_INSTANCES.equals(getDesiredInstanceCount()))
            .fixedInstances(fixedInstances)
            .maxInstances(maxInstances)
            .resizeStrategy(resizeStrategy)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .clusterName(executionData.getClusterName())
            .namespace(setupParams.getNamespace())
            .deploymentType(DeploymentType.KUBERNETES)
            .infraMappingId(setupParams.getInfraMappingId())
            .useIstioRouteRule(useIstioRouteRule);
    if (executionResult != null) {
      ContainerSetupCommandUnitExecutionData setupExecutionData =
          (ContainerSetupCommandUnitExecutionData) executionResult.getCommandExecutionData();
      if (setupExecutionData != null) {
        containerServiceElementBuilder.name(setupExecutionData.getContainerServiceName())
            .previousDaemonSetYaml(setupExecutionData.getPreviousDaemonSetYaml())
            .previousActiveAutoscalers(setupExecutionData.getPreviousActiveAutoscalers());
      }
    }

    if (useAutoscaler) {
      String customMetricYamlEvaluated = null;
      if (isNotBlank(customMetricYamlConfig)) {
        customMetricYamlEvaluated = context.renderExpression(customMetricYamlConfig);
      }
      containerServiceElementBuilder.useAutoscaler(true)
          .minAutoscaleInstances(minAutoscaleInstances)
          .maxAutoscaleInstances(maxAutoscaleInstances)
          .targetCpuUtilizationPercentage(targetCpuUtilizationPercentage)
          .customMetricYamlConfig(customMetricYamlEvaluated);
    }

    return containerServiceElementBuilder.build();
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
        || infrastructureMapping instanceof AzureKubernetesInfrastructureMapping
        || infrastructureMapping instanceof DirectKubernetesInfrastructureMapping;
  }

  @Override
  protected String getDeploymentType() {
    return DeploymentType.KUBERNETES.name();
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getReplicationControllerName() {
    return replicationControllerName;
  }

  public void setReplicationControllerName(String replicationControllerName) {
    this.replicationControllerName = replicationControllerName;
  }

  public String getServiceType() {
    return serviceType.name();
  }

  public void setServiceType(String serviceType) {
    try {
      this.serviceType = KubernetesServiceType.valueOf(serviceType);
    } catch (IllegalArgumentException e) {
      this.serviceType = KubernetesServiceType.None;
    }
  }

  public String getPort() {
    return port.toString();
  }

  public void setPort(String port) {
    this.port = Integer.parseInt(port);
  }

  public String getTargetPort() {
    return targetPort.toString();
  }

  public void setTargetPort(String targetPort) {
    this.targetPort = Integer.parseInt(targetPort);
  }

  public String getPortName() {
    return portName;
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  public String getProtocol() {
    return protocol.name();
  }

  public void setProtocol(String protocol) {
    try {
      this.protocol = KubernetesPortProtocol.valueOf(protocol);
    } catch (IllegalArgumentException e) {
      this.protocol = KubernetesPortProtocol.TCP;
    }
  }

  public String getClusterIP() {
    return clusterIP;
  }

  public void setClusterIP(String clusterIP) {
    this.clusterIP = clusterIP;
  }

  public String getExternalIPs() {
    return externalIPs;
  }

  public void setExternalIPs(String externalIPs) {
    this.externalIPs = externalIPs;
  }

  public String getLoadBalancerIP() {
    return loadBalancerIP;
  }

  public void setLoadBalancerIP(String loadBalancerIP) {
    this.loadBalancerIP = loadBalancerIP;
  }

  public String getNodePort() {
    return nodePort.toString();
  }

  public void setNodePort(String nodePort) {
    this.nodePort = Integer.parseInt(nodePort);
  }

  public String getExternalName() {
    return externalName;
  }

  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }

  public boolean isUseAutoscaler() {
    return useAutoscaler;
  }

  public void setUseAutoscaler(boolean useAutoscaler) {
    this.useAutoscaler = useAutoscaler;
  }

  public int getMinAutoscaleInstances() {
    return minAutoscaleInstances;
  }

  public void setMinAutoscaleInstances(int minAutoscaleInstances) {
    this.minAutoscaleInstances = minAutoscaleInstances;
  }

  public int getMaxAutoscaleInstances() {
    return maxAutoscaleInstances;
  }

  public void setMaxAutoscaleInstances(int maxAutoscaleInstances) {
    this.maxAutoscaleInstances = maxAutoscaleInstances;
  }

  public int getTargetCpuUtilizationPercentage() {
    return targetCpuUtilizationPercentage;
  }

  public void setTargetCpuUtilizationPercentage(int targetCpuUtilizationPercentage) {
    this.targetCpuUtilizationPercentage = targetCpuUtilizationPercentage;
  }

  public String getCustomMetricYamlConfig() {
    return customMetricYamlConfig;
  }

  public void setCustomMetricYamlConfig(String customMetricYamlConfig) {
    this.customMetricYamlConfig = trimYaml(customMetricYamlConfig);
  }

  public boolean isUseIngress() {
    return useIngress;
  }

  public void setUseIngress(boolean useIngress) {
    this.useIngress = useIngress;
  }

  public String getIngressYaml() {
    return ingressYaml;
  }

  public void setIngressYaml(String ingressYaml) {
    this.ingressYaml = trimYaml(ingressYaml);
  }

  public boolean isUseIstioRouteRule() {
    return useIstioRouteRule;
  }

  public void setUseIstioRouteRule(boolean useIstioRouteRule) {
    this.useIstioRouteRule = useIstioRouteRule;
  }
}
