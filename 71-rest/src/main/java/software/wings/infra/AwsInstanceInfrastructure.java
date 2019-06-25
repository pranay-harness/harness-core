package software.wings.infra;

import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.AwsInstanceFilter;

@Data
public class AwsInstanceInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String region;
  private String hostConnectionAttrs;
  private String loadBalancerId;
  @Transient private String loadBalancerName;
  private boolean usePublicDns;
  private boolean useAutoScalingGroup;
  private AwsInstanceFilter awsInstanceFilter;
  private String autoScalingGroupName;
  private boolean setDesiredCapacity;
  private int desiredCapacity;
  private String hostNameConvention;
}
