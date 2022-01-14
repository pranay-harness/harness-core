package io.harness.expression.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;

@OwnedBy(CDC)
public class DateTimeFunctor implements ExpressionFunctor {
  public CustomDate currentDate() {
    return new CustomDate();
  }

  public CustomDateTime currentTime() {
    return new CustomDateTime();
  }
}
