package io.harness.adviser.impl.success;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserParameters;
import io.harness.adviser.AdviserType;
import io.harness.annotations.ProducesAdviser;
import io.harness.registries.adviser.AdviserProducer;

@ProducesAdviser
public class OnSuccessAdviserProducer implements AdviserProducer {
  @Override
  public Adviser produce(AdviserParameters adviserParameters) {
    return OnSuccessAdviser.builder()
        .onSuccessAdviserParameters((OnSuccessAdviserParameters) adviserParameters)
        .build();
  }

  @Override
  public AdviserType getType() {
    return AdviserType.builder().type(AdviserType.ON_SUCCESS).build();
  }
}
