package software.wings.yaml.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePair;
import software.wings.beans.TemplateExpressionYaml;
import software.wings.yaml.BaseYamlWithType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 10/26/17
 */
@TargetModule(HarnessModule._870_YAML_BEANS)
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StepYaml extends BaseYamlWithType {
  private String name;
  private Map<String, Object> properties = new HashMap<>();
  private List<TemplateExpressionYaml> templateExpressions;
  private String templateUri;
  private List<NameValuePair> templateVariables;

  @Builder
  public StepYaml(String type, String name, Map<String, Object> properties,
      List<TemplateExpressionYaml> templateExpressions, String templateUri, List<NameValuePair> templateVariables) {
    super(type);
    this.name = name;
    this.properties = properties;
    this.templateExpressions = templateExpressions;
    this.templateUri = templateUri;
    this.templateVariables = templateVariables;
  }
}
