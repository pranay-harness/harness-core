package io.harness.adviser.impl.ignore;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserParameters;
import io.harness.adviser.AdviserType;
import io.harness.annotations.ProducesAdviser;
import io.harness.annotations.Redesign;
import io.harness.registries.adviser.AdviserProducer;

@Redesign
@ProducesAdviser
public class IgnoreAdviserProducer implements AdviserProducer {
  @Override
  public AdviserType getType() {
    return AdviserType.builder().type(AdviserType.IGNORE).build();
  }

  @Override
  public Adviser produce(AdviserParameters adviserParameters) {
    return IgnoreAdviser.builder().parameters((IgnoreAdviserParameters) adviserParameters).build();
  }
}
