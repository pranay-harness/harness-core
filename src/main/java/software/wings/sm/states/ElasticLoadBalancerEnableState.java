package software.wings.sm.states;

import static software.wings.sm.StateType.E_L_B_ENABLE_HOST;

import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.StringUtils;
import software.wings.sm.ExecutionStatus;
import software.wings.stencils.DefaultValue;

/**
 * Created by peeyushaggarwal on 9/12/16.
 */
public class ElasticLoadBalancerEnableState extends ElasticLoadBalancerBaseState {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public ElasticLoadBalancerEnableState(String name) {
    super(name, E_L_B_ENABLE_HOST.name());
  }

  @Override
  protected ExecutionStatus getExecutionStatus(AmazonWebServiceResult<ResponseMetadata> result, String instanceId) {
    ExecutionStatus status;
    status = ((RegisterInstancesWithLoadBalancerResult) result)
                 .getInstances()
                 .stream()
                 .map(Instance::getInstanceId)
                 .filter(s -> StringUtils.equals(s, instanceId))
                 .findFirst()
                 .isPresent()
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    return status;
  }

  @Override
  protected AmazonWebServiceResult<ResponseMetadata> doOperation(
      String instanceId, AmazonElasticLoadBalancingClient elbClient) {
    RegisterInstancesWithLoadBalancerResult result;
    result = elbClient.registerInstancesWithLoadBalancer(new RegisterInstancesWithLoadBalancerRequest()
                                                             .withLoadBalancerName(getLoadBalancerName())
                                                             .withInstances(new Instance().withInstanceId(instanceId)));
    return result;
  }

  @DefaultValue("US_EAST_1")
  @Attributes(title = "Region")
  @Override
  public Regions getRegion() {
    return super.getRegion();
  }
}
