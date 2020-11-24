package software.wings.beans;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import software.wings.beans.HelmCommandFlagConstants.HelmSubCommand;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmCommandFlag implements NestedAnnotationResolver {
  @NotNull @Expression(ALLOW_SECRETS) private Map<HelmSubCommand, String> valueMap;
}
