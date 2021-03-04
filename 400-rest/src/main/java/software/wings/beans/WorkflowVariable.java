package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowVariable extends NameValuePairAbstractYaml {
  String entityType;

  @Builder
  public WorkflowVariable(
      String entityType, String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls) {
    super(name, value, valueType, allowedValueYamls);
    this.entityType = entityType;
  }
}
