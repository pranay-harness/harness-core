package software.wings.sm.states;

import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;
import static software.wings.sm.StateType.ECS_BG_SERVICE_SETUP_ROUTE53;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.command.CommandExecutionResult;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.sm.ExecutionContext;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsBlueGreenServiceSetupRoute53DNS extends ContainerServiceSetup {
  private static final Logger logger = LoggerFactory.getLogger(EcsBlueGreenServiceSetupRoute53DNS.class);
  private String commandName = "Setup Service Cluster";
  @Inject private transient EcsStateHelper ecsStateHelper;

  @Getter @Setter private String roleArn;
  @Getter @Setter private String targetPort;
  @Getter @Setter private String ecsServiceName;
  @Getter @Setter private String targetContainerName;
  @Getter @Setter private String serviceDiscoveryService1JSON;
  @Getter @Setter private String serviceDiscoveryService2JSON;
  @Getter @Setter private String parentRecordHostedZoneId;
  @Getter @Setter private String parentRecordName;

  public EcsBlueGreenServiceSetupRoute53DNS(String name) {
    super(name, ECS_BG_SERVICE_SETUP_ROUTE53.name());
  }

  @Override
  protected boolean isValidInfraMapping(InfrastructureMapping infrastructureMapping) {
    return infrastructureMapping instanceof EcsInfrastructureMapping;
  }

  @Override
  protected ContainerSetupParams buildContainerSetupParams(ExecutionContext context, String serviceName,
      ImageDetails imageDetails, Application app, Environment env, Service service,
      ContainerInfrastructureMapping infrastructureMapping, ContainerTask containerTask, String clusterName) {
    int serviceSteadyStateTimeout =
        getServiceSteadyStateTimeout() > 0 ? getServiceSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT;

    EcsServiceSpecification serviceSpecification =
        serviceResourceService.getEcsServiceSpecification(app.getUuid(), service.getUuid());
    return ecsStateHelper.buildContainerSetupParams(context,
        EcsSetupStateConfig.builder()
            .useRoute53DNSSwap(true)
            .serviceDiscoveryService1JSON(serviceDiscoveryService1JSON)
            .serviceDiscoveryService2JSON(serviceDiscoveryService2JSON)
            .parentRecordHostedZoneId(parentRecordHostedZoneId)
            .parentRecordName(parentRecordName)
            .blueGreen(true)
            .app(app)
            .env(env)
            .service(service)
            .infrastructureMapping(infrastructureMapping)
            .clusterName(clusterName)
            .containerTask(containerTask)
            .ecsServiceName(ecsServiceName)
            .imageDetails(imageDetails)
            .roleArn(roleArn)
            .serviceSteadyStateTimeout(serviceSteadyStateTimeout)
            .targetContainerName(targetContainerName)
            .targetPort(targetPort)
            .useLoadBalancer(false)
            .serviceName(serviceName)
            .ecsServiceSpecification(serviceSpecification)
            .isDaemonSchedulingStrategy(false)
            .build());
  }

  protected String getClusterNameFromContextElement(ExecutionContext context) {
    return super.getClusterNameFromContextElement(context).split("/")[1];
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  @Override
  protected String getDeploymentType() {
    return DeploymentType.ECS.name();
  }

  @Override
  protected ContainerServiceElement buildContainerServiceElement(ExecutionContext context,
      CommandExecutionResult executionResult, ExecutionStatus status, ImageDetails imageDetails) {
    return ecsStateHelper.buildContainerServiceElement(context, executionResult, status, imageDetails,
        getMaxInstances(), getFixedInstances(), getDesiredInstanceCount(), getResizeStrategy(),
        getServiceSteadyStateTimeout(), logger);
  }
}