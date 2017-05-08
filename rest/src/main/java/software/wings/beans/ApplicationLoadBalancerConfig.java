package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.api.LoadBalancerConfig;

/**
 * Created by anubhaw on 2/10/17.
 */
@JsonTypeName("ALB")
public class ApplicationLoadBalancerConfig extends LoadBalancerConfig {
  @Attributes(title = "Load Balancer Target Group ARN", required = true) private String loadBalancerTargetGroupArn;

  /**
   * Instantiates a new setting value.
   */
  public ApplicationLoadBalancerConfig() {
    super(SettingVariableTypes.ALB.name());
  }

  /**
   * Gets load balancer target group arn.
   *
   * @return the load balancer target group arn
   */
  public String getLoadBalancerTargetGroupArn() {
    return loadBalancerTargetGroupArn;
  }

  /**
   * Sets load balancer target group arn.
   *
   * @param loadBalancerTargetGroupArn the load balancer target group arn
   */
  public void setLoadBalancerTargetGroupArn(String loadBalancerTargetGroupArn) {
    this.loadBalancerTargetGroupArn = loadBalancerTargetGroupArn;
  }
}
