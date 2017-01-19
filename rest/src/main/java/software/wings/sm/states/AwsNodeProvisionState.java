package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static software.wings.api.ServiceInstanceIdsParam.ServiceInstanceIdsParamBuilder.aServiceInstanceIdsParam;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ServiceInstance;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by rishi on 1/12/17.
 */
public class AwsNodeProvisionState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsNodeProvisionState.class);

  private String environmentId;
  private String serviceId;
  private String computeProviderId;

  @Attributes(title = "Host specification (Launch Configuration Name") private String launcherConfigName;

  @Attributes(title = "Number of instances") private int instanceCount;

  @Inject private InfrastructureMappingService infrastructureMappingService;

  public AwsNodeProvisionState(String name) {
    super(name, StateType.AWS_NODE_PROVISION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String appId = ((ExecutionContextImpl) context).getApp().getUuid();
    String envId = ((ExecutionContextImpl) context).getEnv().getUuid();

    logger.info("serviceId : {}, computeProviderId: {}", serviceId, computeProviderId);

    List<ServiceInstance> serviceInstances = infrastructureMappingService.provisionNodes(
        appId, serviceId, envId, computeProviderId, launcherConfigName, instanceCount);
    List<String> serviceInstancesIds = serviceInstances.stream().map(ServiceInstance::getUuid).collect(toList());
    return anExecutionResponse()
        .addElement(aServiceInstanceIdsParam().withInstanceIds(serviceInstancesIds).withServiceId(serviceId).build())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(String environmentId) {
    this.environmentId = environmentId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getComputeProviderId() {
    return computeProviderId;
  }

  public void setComputeProviderId(String computeProviderId) {
    this.computeProviderId = computeProviderId;
  }

  public int getInstanceCount() {
    return instanceCount;
  }

  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  public String getLauncherConfigName() {
    return launcherConfigName;
  }

  public void setLauncherConfigName(String launcherConfigName) {
    this.launcherConfigName = launcherConfigName;
  }
}
