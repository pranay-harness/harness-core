package software.wings.beans.customdeployment;

import software.wings.beans.Variable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomDeploymentTypeDTO {
  /*
  Id of the custom deployment template
   */
  private String uuid;
  private String name;
  private List<Variable> infraVariables;
}
