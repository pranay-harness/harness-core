package software.wings.utils;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.Expression;

import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@Builder
@TargetModule(HarnessModule._957_CG_BEANS)
public class EmailParams {
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String subject;
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String toAddress;
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String ccAddress;
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String body;
}
