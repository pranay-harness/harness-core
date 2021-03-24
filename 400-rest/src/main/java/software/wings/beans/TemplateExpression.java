package software.wings.beans;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by sgurubelli on 8/11/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateExpression {
  private String fieldName;
  private String expression;
  @Default private boolean expressionAllowed = true; // Can this template expression can contain other expression
  private String description;
  private boolean mandatory;
  @Default private Map<String, Object> metadata = new HashMap<>();
}
