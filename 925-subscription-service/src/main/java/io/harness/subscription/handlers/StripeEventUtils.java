package io.harness.subscription.handlers;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StripeEventUtils {
  public <T> T convertEvent(Event event, Class<T> clazz) {
    EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
    return (T) dataObjectDeserializer.getObject().get();
  }
}
