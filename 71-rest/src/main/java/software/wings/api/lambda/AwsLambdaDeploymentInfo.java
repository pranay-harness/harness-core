package software.wings.api.lambda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.DeploymentInfo;
import software.wings.beans.Tag;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaDeploymentInfo extends DeploymentInfo {
  private String functionName;
  private String functionArn;
  private String version;
  private List<String> aliases;
  private List<Tag> tags;
  private String artifactId;
}
