package software.wings.yaml.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;
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
public class StepYaml extends BaseEntityYaml {
  private String name;
  private boolean rollback;
  private List<NameValuePair.Yaml> properties = new ArrayList<>();
  private List<TemplateExpression.Yaml> templateExpressions;

  public static final class Builder {
    private String name;
    private boolean rollback;
    private List<NameValuePair.Yaml> properties = new ArrayList<>();
    private List<TemplateExpression.Yaml> templateExpressions;
    private String type;

    private Builder() {}

    public static Builder aYaml() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public Builder withProperties(List<NameValuePair.Yaml> properties) {
      this.properties = properties;
      return this;
    }

    public Builder withTemplateExpressions(List<Yaml> templateExpressions) {
      this.templateExpressions = templateExpressions;
      return this;
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder but() {
      return aYaml()
          .withName(name)
          .withRollback(rollback)
          .withProperties(properties)
          .withType(type)
          .withTemplateExpressions(templateExpressions);
    }

    public StepYaml build() {
      StepYaml yaml = new StepYaml();
      yaml.setName(name);
      yaml.setRollback(rollback);
      yaml.setProperties(properties);
      yaml.setTemplateExpressions(templateExpressions);
      yaml.setType(type);
      return yaml;
    }
  }
}
