package software.wings.helpers.ext.jenkins.model;

import com.offbytwo.jenkins.model.BaseModel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ParametersDefinitionProperty extends BaseModel {
  private String name;
  private String description;
  private DefaultParameterValue defaultParameterValue;
  private String type;
  private List<String> choices;

  @Data
  @Builder
  public static class DefaultParameterValue {
    private String name;
    private String value;
  }
}
