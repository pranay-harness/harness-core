package software.wings.helpers.ext.jenkins.model;

import com.offbytwo.jenkins.model.BaseModel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class JobProperty extends BaseModel {
  private List<ParametersDefinitionProperty> parameterDefinitions;
}
