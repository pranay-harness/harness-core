package software.wings.helpers.ext.jenkins.model;

import com.offbytwo.jenkins.model.BaseModel;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class JobProperty extends BaseModel {
  private List<ParametersDefinitionProperty> parameterDefinitions;
}
