package io.harness.eventframework.dms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.observer.AbstractRemoteInformer;
import io.harness.observer.RemoteObserverInformer;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.lang.reflect.Method;

@OwnedBy(HarnessTeam.DEL)
public class DmsObserverEventProducer extends AbstractRemoteInformer implements RemoteObserverInformer {
  @Inject
  public DmsObserverEventProducer(
      KryoSerializer kryoSerializer, @Named(EventsFrameworkConstants.OBSERVER_EVENT_CHANNEL) Producer eventProducer) {
    super(kryoSerializer, eventProducer);
  }

  @Override
  public void sendEvent(Method method, Class<?> subjectClazz, Object... params) {
    super.fireInform(method, subjectClazz, params);
  }
}
