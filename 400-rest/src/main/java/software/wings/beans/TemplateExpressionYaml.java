package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class TemplateExpressionYaml extends BaseYaml {
  private String fieldName;
  private String expression;
  private List<NameValuePairYaml> metadata = Lists.newArrayList();

  public static final class Builder {
    private String fieldName;
    private String expression;
    private List<NameValuePairYaml> metadata = Lists.newArrayList();

    private Builder() {}

    public static Builder aYaml() {
      return new Builder();
    }

    public Builder withFieldName(String fieldName) {
      this.fieldName = fieldName;
      return this;
    }

    public Builder withExpression(String expression) {
      this.expression = expression;
      return this;
    }

    public Builder withMetadata(List<NameValuePairYaml> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder but() {
      return aYaml().withFieldName(fieldName).withExpression(expression).withMetadata(metadata);
    }

    public TemplateExpressionYaml build() {
      TemplateExpressionYaml yaml = new TemplateExpressionYaml();
      yaml.setFieldName(fieldName);
      yaml.setExpression(expression);
      yaml.setMetadata(metadata);
      return yaml;
    }
  }
}
