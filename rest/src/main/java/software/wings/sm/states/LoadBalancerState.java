package software.wings.sm.states;

import static software.wings.api.ElbStateExecutionData.Builder.anElbStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public class LoadBalancerState extends State {
  @Transient @Inject private transient InfrastructureMappingService infrastructureMappingService;

  @Transient @Inject private transient SettingsService settingsService;

  @Transient @Inject private transient AwsHelperService awsHelperService;

  @Attributes(title = "Operation") private Operation operation;

  @DefaultValue("Instance") @Attributes(title = "Entity") private Entity entity;

  @Attributes(title = "Custom Entity") @SchemaIgnore private String custom;

  public LoadBalancerState(String name) {
    super(name, StateType.LOAD_BALANCER.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionStatus status = ExecutionStatus.SUCCESS;

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);

    AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infrastructureMappingService.get(
        context.getAppId(), phaseElement.getInfraMappingId());

    awsInfrastructureMapping.getComputeProviderSettingId();

    String loadBalancerName = awsInfrastructureMapping.getLoadBalancerId();

    SettingAttribute settingAttribute =
        settingsService.get(context.getAppId(), awsInfrastructureMapping.getComputeProviderSettingId());
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = awsHelperService.getClassicElbClient(
        Regions.fromName(awsInfrastructureMapping.getRegion()), awsConfig.getAccessKey(), awsConfig.getSecretKey());

    InstanceElement instance = context.getContextElement(ContextElementType.INSTANCE);
    String instanceId = instance.getHostElement().getInstanceId();

    String errorMessage = "";

    try {
      boolean result = operation == Operation.Enable
          ? amazonElasticLoadBalancingClient
                .registerInstancesWithLoadBalancer(new RegisterInstancesWithLoadBalancerRequest()
                                                       .withLoadBalancerName(loadBalancerName)
                                                       .withInstances(new Instance(instanceId)))
                .getInstances()
                .contains(instanceId)
          : !amazonElasticLoadBalancingClient
                 .deregisterInstancesFromLoadBalancer(new DeregisterInstancesFromLoadBalancerRequest()
                                                          .withLoadBalancerName(loadBalancerName)
                                                          .withInstances(new Instance(instanceId)))
                 .getInstances()
                 .contains(instanceId);
      status = result ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    } catch (Exception e) {
      status = ExecutionStatus.ERROR;
      errorMessage = e.getMessage();
    }

    return anExecutionResponse()
        .withStateExecutionData(anElbStateExecutionData().withHostName(instance.getHostElement().getHostName()).build())
        .withExecutionStatus(status)
        .withErrorMessage(errorMessage)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Getter for property 'operation'.
   *
   * @return Value for property 'operation'.
   */
  public Operation getOperation() {
    return operation;
  }

  /**
   * Setter for property 'operation'.
   *
   * @param operation Value to set for property 'operation'.
   */
  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public String getCustom() {
    return custom;
  }

  public void setCustom(String custom) {
    this.custom = custom;
  }

  /**
   * Getter for property 'entity'.
   *
   * @return Value for property 'entity'.
   */
  public Entity getEntity() {
    return entity;
  }

  /**
   * Setter for property 'entity'.
   *
   * @param entity Value to set for property 'entity'.
   */
  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public enum Operation { Enable, Disable }

  private enum Entity {
    Instance("${instance}"),
    Custom("");

    private String expression;

    Entity(String expression) {
      this.expression = expression;
    }

    public String getExpression() {
      return expression;
    }
  }
}
