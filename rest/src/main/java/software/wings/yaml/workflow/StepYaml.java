package software.wings.yaml.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.yaml.BaseEntityYaml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 10/26/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StepYaml extends BaseEntityYaml {
  private String name;
  private boolean rollback;
  private Map<String, Object> properties = new HashMap<>();
  private List<TemplateExpression.Yaml> templateExpressions;

  @Builder
  public StepYaml(String type, String harnessApiVersion, String name, boolean rollback, Map<String, Object> properties,
      List<Yaml> templateExpressions) {
    super(type, harnessApiVersion);
    this.name = name;
    this.rollback = rollback;
    this.properties = properties;
    this.templateExpressions = templateExpressions;
  }
}
