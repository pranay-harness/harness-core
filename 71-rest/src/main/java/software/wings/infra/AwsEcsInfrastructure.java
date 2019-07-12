package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;

import java.util.List;

@JsonTypeName("AWS_ECS")
@Data
public class AwsEcsInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String region;
  private String vpcId;
  private List<String> subnetIds;
  private List<String> securityGroupIds;
  private boolean assignPublicIp;
  private String executionRole;
  private String launchType;

  @SchemaIgnore private String type;
  @SchemaIgnore private String role;
  @SchemaIgnore private int diskSize;
  @SchemaIgnore private String ami;
  @SchemaIgnore private int numberOfNodes;
}
