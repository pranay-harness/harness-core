package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.common.Constants.CONTAINER_SYNC_CALL_TIMEOUT;
import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.KUBERNETES_SETUP;
import static software.wings.utils.Switch.unhandled;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.ContainerSetupCommandUnitExecutionData;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.command.KubernetesSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ContainerService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.KubernetesConvention;

import java.util.List;

/**
 * Created by brett on 3/1/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesSetup extends ContainerServiceSetup {
  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/KubernetesRepCtrlSetup.js

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

  @Inject @Transient private transient DelegateProxyFactory delegateProxyFactory;

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, ContainerInfrastructureMapping infrastructureMapping,
      ContainerTask containerTask, String clusterName) {
    String controllerNamePrefix = isNotBlank(replicationControllerName)
        ? KubernetesConvention.normalize(context.renderExpression(replicationControllerName))
        : KubernetesConvention.getControllerNamePrefix(app.getName(), serviceName, env.getName());

    boolean isDaemonSet = false;
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
      isDaemonSet = kubernetesContainerTask.checkDaemonSet();
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
        getServiceSteadyStateTimeout() > 0 ? (int) getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;
    ContextData contextData = buildContextData(context, app, infrastructureMapping, controllerNamePrefix, clusterName);
    String previousDaemonSetYaml = isDaemonSet ? getDaemonSetYaml(contextData) : null;
    List<String> activeAutoscalers = isDaemonSet ? null : getActiveAutoscalers(contextData);

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
        .withPreviousDaemonSetYaml(previousDaemonSetYaml)
        .withActiveAutoscalers(activeAutoscalers)
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
        .build();
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(
      ExecutionContext context, CommandExecutionResult executionResult, ExecutionStatus status) {
    CommandStateExecutionData executionData = (CommandStateExecutionData) context.getStateExecutionData();
    KubernetesSetupParams setupParams = (KubernetesSetupParams) executionData.getContainerSetupParams();
    int maxInstances = getMaxInstances() == 0 ? DEFAULT_MAX : getMaxInstances();
    int fixedInstances = getFixedInstances() == 0 ? maxInstances : getFixedInstances();
    ResizeStrategy resizeStrategy = getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy();
    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? (int) getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;

    String customMetricYamlEvaluated = null;
    if (isNotBlank(customMetricYamlConfig)) {
      customMetricYamlEvaluated = context.renderExpression(customMetricYamlConfig);
    }

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
        containerServiceElementBuilder.name(setupExecutionData.getContainerServiceName());
      }
    }

    if (useAutoscaler) {
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

  private String getDaemonSetYaml(ContextData contextData) {
    return getContainerService(contextData.app).getDaemonSetYaml(contextData.containerServiceParams);
  }

  private List<String> getActiveAutoscalers(ContextData contextData) {
    return getContainerService(contextData.app).getActiveAutoscalers(contextData.containerServiceParams);
  }

  private ContainerService getContainerService(Application app) {
    return delegateProxyFactory.get(ContainerService.class,
        aContext()
            .withAccountId(app.getAccountId())
            .withAppId(app.getUuid())
            .withTimeout(CONTAINER_SYNC_CALL_TIMEOUT)
            .build());
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

  /**
   * Gets service type.
   */
  public String getServiceType() {
    return serviceType.name();
  }

  /**
   * Sets service type.
   */
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
    this.customMetricYamlConfig = customMetricYamlConfig;
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
    this.ingressYaml = ingressYaml;
  }

  public boolean isUseIstioRouteRule() {
    return useIstioRouteRule;
  }

  public void setUseIstioRouteRule(boolean useIstioRouteRule) {
    this.useIstioRouteRule = useIstioRouteRule;
  }

  private ContextData buildContextData(ExecutionContext context, Application app,
      InfrastructureMapping infrastructureMapping, String controllerNamePrefix, String clusterName) {
    return new ContextData(context, app, infrastructureMapping, controllerNamePrefix, clusterName, this);
  }

  protected static class ContextData {
    final Application app;
    final ContainerServiceParams containerServiceParams;

    ContextData(ExecutionContext context, Application app, InfrastructureMapping infrastructureMapping,
        String controllerNamePrefix, String clusterName, KubernetesSetup setup) {
      this.app = app;
      SettingAttribute settingAttribute = infrastructureMapping instanceof DirectKubernetesInfrastructureMapping
          ? aSettingAttribute()
                .withValue(((DirectKubernetesInfrastructureMapping) infrastructureMapping).createKubernetesConfig())
                .build()
          : setup.settingsService.get(infrastructureMapping.getComputeProviderSettingId());

      String subscriptionId = null;
      String resourceGroup = null;
      String namespace = null;
      if (infrastructureMapping instanceof AzureKubernetesInfrastructureMapping) {
        subscriptionId = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getSubscriptionId();
        resourceGroup = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getResourceGroup();
        namespace = ((AzureKubernetesInfrastructureMapping) infrastructureMapping).getNamespace();
      }

      List<EncryptedDataDetail> encryptionDetails = setup.secretManager.getEncryptionDetails(
          (Encryptable) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
      containerServiceParams = ContainerServiceParams.builder()
                                   .settingAttribute(settingAttribute)
                                   .containerServiceName(controllerNamePrefix)
                                   .encryptionDetails(encryptionDetails)
                                   .clusterName(clusterName)
                                   .subscriptionId(subscriptionId)
                                   .resourceGroup(resourceGroup)
                                   .namespace(namespace)
                                   .build();
    }
  }
}
