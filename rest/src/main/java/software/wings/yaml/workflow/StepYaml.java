package software.wings.yaml.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.NameValuePair;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rktummala on 10/26/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StepYaml extends BaseEntityYaml {
  private String name;
  private boolean rollback;
  private List<NameValuePair.Yaml> properties = new ArrayList<>();
  private List<TemplateExpression.Yaml> templateExpressions;

  @Builder
  public StepYaml(String type, String harnessApiVersion, String name, boolean rollback,
      List<NameValuePair.Yaml> properties, List<Yaml> templateExpressions) {
    super(type, harnessApiVersion);
    this.name = name;
    this.rollback = rollback;
    this.properties = properties;
    this.templateExpressions = templateExpressions;
  }
}
