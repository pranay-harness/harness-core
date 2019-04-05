package software.wings.beans.template;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.Variable;

import java.util.Map;

@Value
@Builder
public class ReferencedTemplate {
  private TemplateReference templateReference;
  private Map<String, Variable> variableMapping;
}
