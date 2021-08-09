package software.wings.beans.template;

import software.wings.beans.Variable;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReferencedTemplate {
  private TemplateReference templateReference;
  private Map<String, Variable> variableMapping;
}
