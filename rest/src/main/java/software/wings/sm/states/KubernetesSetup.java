package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.ResizeStrategy.RESIZE_NEW_FIRST;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.sm.StateType.KUBERNETES_SETUP;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerServiceElement;
import software.wings.api.ContainerServiceElement.ContainerServiceElementBuilder;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
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

  static final int DEFAULT_STEADY_STATE_TIMEOUT = 10;

  private String replicationControllerName;
  private KubernetesServiceType serviceType;
  private Integer port;
  private Integer targetPort;
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
      isDaemonSet = kubernetesContainerTask.isDaemonSet();
    }

    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? (int) getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;
    ContextData contextData = buildContextData(context, app, infrastructureMapping, controllerNamePrefix, clusterName);
    String previousDaemonSetYaml = isDaemonSet ? getDaemonSetYaml(contextData) : null;
    List<String> activeAutoscalers = isDaemonSet ? null : getActiveAutoscalers(contextData);

    return aKubernetesSetupParams()
        .withAppName(app.getName())
        .withEnvName(env.getName())
        .withServiceName(serviceName)
        .withClusterName(clusterName)
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
        .withControllerNamePrefix(controllerNamePrefix)
        .withPreviousDaemonSetYaml(previousDaemonSetYaml)
        .withActiveAutoscalers(activeAutoscalers)
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .withUseAutoscaler(useAutoscaler)
        .withMinAutoscaleInstances(minAutoscaleInstances)
        .withMaxAutoscaleInstances(maxAutoscaleInstances)
        .withTargetCpuUtilizationPercentage(targetCpuUtilizationPercentage)
        .build();
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(
      CommandStateExecutionData executionData, CommandExecutionResult executionResult, ExecutionStatus status) {
    KubernetesSetupParams setupParams = (KubernetesSetupParams) executionData.getContainerSetupParams();
    int maxInstances = getMaxInstances() == 0 ? 2 : getMaxInstances();
    int fixedInstances = getFixedInstances() == 0 ? maxInstances : getFixedInstances();
    ResizeStrategy resizeStrategy = getResizeStrategy() == null ? RESIZE_NEW_FIRST : getResizeStrategy();
    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? (int) getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;
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
            .infraMappingId(setupParams.getInfraMappingId());
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
          .targetCpuUtilizationPercentage(targetCpuUtilizationPercentage);
    }

    return containerServiceElementBuilder.build();
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof GcpKubernetesInfrastructureMapping
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
    return delegateProxyFactory.get(
        ContainerService.class, aContext().withAccountId(app.getAccountId()).withAppId(app.getUuid()).build());
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
      List<EncryptedDataDetail> encryptionDetails = setup.secretManager.getEncryptionDetails(
          (Encryptable) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
      containerServiceParams = ContainerServiceParams.builder()
                                   .settingAttribute(settingAttribute)
                                   .containerServiceName(controllerNamePrefix)
                                   .encryptionDetails(encryptionDetails)
                                   .clusterName(clusterName)
                                   .build();
    }
  }
}
