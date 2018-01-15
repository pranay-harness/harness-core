package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.command.KubernetesSetupParams.KubernetesSetupParamsBuilder.aKubernetesSetupParams;
import static software.wings.sm.StateType.KUBERNETES_DAEMON_SET_ROLLBACK;
import static software.wings.sm.states.KubernetesReplicationControllerSetup.DEFAULT_STEADY_STATE_TIMEOUT;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ImageDetails;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.KubernetesPortProtocol;
import software.wings.beans.container.KubernetesServiceType;
import software.wings.common.Constants;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.KubernetesConvention;

import java.util.stream.Collectors;

/**
 * Created by brett on 12/18/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesDaemonSetRollback extends ContainerServiceSetup {
  // *** Note: UI Schema specified in wingsui/src/containers/WorkflowEditor/custom/KubernetesRepCtrlSetup.js

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
  private String commandName = "Setup Replication Controller";

  /**
   * Instantiates a new state.
   */
  public KubernetesDaemonSetRollback(String name) {
    super(name, KUBERNETES_DAEMON_SET_ROLLBACK.name());
  }

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, ContainerInfrastructureMapping infrastructureMapping,
      ContainerTask containerTask, String clusterName) {
    String controllerName = isNotEmpty(replicationControllerName)
        ? KubernetesConvention.normalize(context.renderExpression(replicationControllerName))
        : KubernetesConvention.getControllerNamePrefix(app.getName(), serviceName, env.getName());

    if (containerTask != null) {
      KubernetesContainerTask kubernetesContainerTask = (KubernetesContainerTask) containerTask;
      kubernetesContainerTask.getContainerDefinitions()
          .stream()
          .filter(cd -> isNotEmpty(cd.getCommands()))
          .forEach(cd
              -> cd.setCommands(cd.getCommands().stream().map(context::renderExpression).collect(Collectors.toList())));
    }

    ContainerRollbackRequestElement rollbackElement =
        context.getContextElement(ContextElementType.PARAM, Constants.CONTAINER_ROLLBACK_REQUEST_PARAM);

    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? (int) getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;
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
        .withControllerNamePrefix(controllerName)
        .withRollbackDaemonSet(true)
        .withPreviousDaemonSetYaml(rollbackElement.getPreviousDaemonSetYaml())
        .withServiceSteadyStateTimeout(serviceSteadyStateTimeout)
        .build();
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(
      CommandStateExecutionData executionData, CommandExecutionResult executionResult, ExecutionStatus status) {
    return null;
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
}
