package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@JsonTypeName("AWS_AWS_CODEDEPLOY")
@Data
public class CodeDeployInfrastructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String region;
  @NotEmpty private String applicationName;
  @NotEmpty private String deploymentGroup;
  private String deploymentConfig;
  private String hostNameConvention;
}
