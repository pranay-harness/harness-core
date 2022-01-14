package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomDeploymentTypeInfo extends DeploymentInfo {
  private String instanceFetchScript;
  private String scriptOutput;
  private List<String> tags;
}
