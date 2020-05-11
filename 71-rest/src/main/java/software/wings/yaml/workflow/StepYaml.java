package software.wings.yaml.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.NameValuePair;
import software.wings.beans.TemplateExpression;
import software.wings.beans.TemplateExpression.Yaml;
import software.wings.yaml.BaseYamlWithType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 10/26/17
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StepYaml extends BaseYamlWithType {
  private String name;
  private Map<String, Object> properties = new HashMap<>();
  private List<TemplateExpression.Yaml> templateExpressions;
  private String templateUri;
  private List<NameValuePair> templateVariables;

  @Builder
  public StepYaml(String type, String name, Map<String, Object> properties, List<Yaml> templateExpressions,
      String templateUri, List<NameValuePair> templateVariables) {
    super(type);
    this.name = name;
    this.properties = properties;
    this.templateExpressions = templateExpressions;
    this.templateUri = templateUri;
    this.templateVariables = templateVariables;
  }
}
