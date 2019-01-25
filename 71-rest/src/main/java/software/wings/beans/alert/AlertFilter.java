package software.wings.beans.alert;

import lombok.NonNull;
import lombok.Value;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;

@Value
@ParametersAreNonnullByDefault
public class AlertFilter {
  public static final String ENVIRONMENTS_KEY = "envIds";
  public static final String APPLICATIONS_KEY = "appIds";

  @NonNull Operator operator;
  @NonNull AlertType alertType;
  @NonNull Map<String, Set<String>> conditions;

  public boolean hasCondition(String conditionKey) {
    return conditions.containsKey(Objects.requireNonNull(conditionKey));
  }

  public Set<String> getConditionValue(String conditionKey) {
    return conditions.getOrDefault(Objects.requireNonNull(conditionKey), Collections.emptySet());
  }

  private enum Operator { MATCHING, NOT_MATCHING }
}
