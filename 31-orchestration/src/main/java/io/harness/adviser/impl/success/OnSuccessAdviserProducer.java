package io.harness.adviser.impl.success;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserParameters;
import io.harness.adviser.AdviserType;
import io.harness.registries.adviser.AdviserProducer;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnSuccessAdviserProducer implements AdviserProducer {
  @Builder.Default AdviserType type = AdviserType.builder().type(AdviserType.ON_SUCCESS).build();

  @Override
  public Adviser produce(AdviserParameters adviserParameters) {
    return OnSuccessAdviser.builder()
        .onSuccessAdviserParameters((OnSuccessAdviserParameters) adviserParameters)
        .build();
  }
}
