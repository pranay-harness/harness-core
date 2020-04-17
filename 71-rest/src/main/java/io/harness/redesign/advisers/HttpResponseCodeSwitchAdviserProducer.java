package io.harness.redesign.advisers;

import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserParameters;
import io.harness.adviser.AdviserType;
import io.harness.annotations.ProducesAdviser;
import io.harness.annotations.Redesign;
import io.harness.registries.adviser.AdviserProducer;

@Redesign
@ProducesAdviser
public class HttpResponseCodeSwitchAdviserProducer implements AdviserProducer {
  @Override
  public AdviserType getType() {
    return AdviserType.builder().type("HTTP_RESPONSE_CODE_SWITCH").build();
  }

  @Override
  public Adviser produce(AdviserParameters adviserParameters) {
    return HttpResponseCodeSwitchAdviser.builder()
        .parameters((HttpResponseCodeSwitchAdviserParameters) adviserParameters)
        .build();
  }
}
