package software.wings.beans.customdeployment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.Variable;

import java.util.List;

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
