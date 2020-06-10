package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class Criteria {
  @Setter Map<String, List<String>> conditions;
  @Getter @Setter ConditionalOperator operator;

  public Criteria() {
    conditions = new HashMap<>();
    operator = ConditionalOperator.AND;
  }

  public Map<String, List<String>> fetchConditions() {
    return this.conditions;
  }

  public String conditionsString() {
    if (isEmpty(conditions)) {
      return "";
    }
    return conditions.entrySet()
        .stream()
        .map(condition
            -> StringUtils.capitalize(condition.getKey()) + " should be "
                + (condition.getValue().size() > 1 ? "any of " + String.join("/", condition.getValue())
                                                   : condition.getValue().get(0)))
        .collect(Collectors.joining(" " + operator.name() + ",\n"));
  }

  public boolean satisfied(Map<String, String> currentStatus) {
    if (isNotEmpty(conditions)) {
      List<Boolean> truthValues =
          conditions.entrySet()
              .stream()
              .map(condition -> condition.getValue().contains(currentStatus.get(condition.getKey())))
              .collect(Collectors.toList());
      return operator.applyOperator(truthValues);
    }
    return false;
  }
}
