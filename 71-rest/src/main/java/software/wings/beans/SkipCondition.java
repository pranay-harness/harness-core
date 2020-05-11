package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HarnessStringUtils.join;

import com.google.common.annotations.VisibleForTesting;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Builder
public class SkipCondition {
  @VisibleForTesting enum SkipConditionType { ALWAYS_SKIP, DO_NOT_SKIP, CONDITIONAL_SKIP }
  @NotNull @Getter private SkipConditionType type;
  @Getter private String expression;

  private static SkipConditionBuilder builder() {
    return new SkipConditionBuilder();
  }

  public static SkipCondition getInstanceForAssertion(String disableAssertion) {
    if (disableAssertion == null) {
      return SkipCondition.builder().type(SkipConditionType.DO_NOT_SKIP).build();
    } else if (disableAssertion.equals("true")) {
      return SkipCondition.builder().type(SkipConditionType.ALWAYS_SKIP).build();
    } else {
      return SkipCondition.builder().type(SkipConditionType.CONDITIONAL_SKIP).expression(disableAssertion).build();
    }
  }

  public String fetchDisableAssertion() {
    switch (type) {
      case ALWAYS_SKIP:
        return "true";
      case DO_NOT_SKIP:
        return null;
      case CONDITIONAL_SKIP:
        return expression;
      default:
        throw new InvalidRequestException(join("Invalid Skip condition type", type.name()));
    }
  }
}
