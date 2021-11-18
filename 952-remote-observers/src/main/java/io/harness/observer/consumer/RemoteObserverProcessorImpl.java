package io.harness.observer.consumer;

import static io.harness.observer.RemoteObserverConstants.SUBJECT_CLASS_NAME;

import io.harness.eventsframework.NgEventLogContext;
import io.harness.eventsframework.consumer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.logging.AutoLogContext;
import io.harness.logging.AutoLogContext.OverrideBehavior;
import io.harness.observer.Informant;
import io.harness.observer.Informant.InformantCase;
import io.harness.observer.Observer;
import io.harness.observer.RemoteObserver;
import io.harness.reflection.ReflectionUtils;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.protobuf.InvalidProtocolBufferException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }), access = AccessLevel.PRIVATE)
@Slf4j
public class RemoteObserverProcessorImpl implements RemoteObserverProcessor {
  Injector injector;
  KryoSerializer kryoSerializer;

  @Override
  public boolean process(Message message, Map<String, RemoteObserver> remoteObserverMap) {
    try (AutoLogContext ignore1 = new NgEventLogContext(message.getId(), OverrideBehavior.OVERRIDE_ERROR)) {
      final Map<String, String> metadataMap = message.getMessage().getMetadataMap();
      final String subjectClassName = metadataMap.get(SUBJECT_CLASS_NAME);
      final RemoteObserver remoteObserver = remoteObserverMap.getOrDefault(subjectClassName, null);
      if (remoteObserver != null) {
        informAllObservers(message, remoteObserver);
      }
    }
    return true;
  }

  private void informAllObservers(Message message, RemoteObserver remoteObserver) {
    final List<Class<Observer>> observers = remoteObserver.getObservers();
    Informant informant = getInformant(message);
    final String methodName = informant.getMethodName();
    observers.forEach(observer -> inform(message, informant, methodName, observer)

    );
  }

  private void inform(Message message, Informant informant, String methodName, Class<Observer> observer) {
    final Observer observerClassObject = injector.getInstance(observer);
    final Method method = ReflectionUtils.getMethod(observerClassObject.getClass(), methodName);
    final InformantCase informantCase = informant.getInformantCase();
    final int number = informantCase.getNumber();
    try {
      switch (number) {
        case 0:
          method.invoke(observerClassObject);
          break;
        case 1:
          final Object param11 = kryoSerializer.asObject(informant.getInformant1().getParam1().toByteArray());
          method.invoke(observerClassObject, param11);
          break;
        case 2:
          final Object param21 = kryoSerializer.asObject(informant.getInformant2().getParam1().toByteArray());
          final Object param22 = kryoSerializer.asObject(informant.getInformant2().getParam2().toByteArray());
          method.invoke(observerClassObject, param21, param22);
          break;
        case 3:
          final Object param31 = kryoSerializer.asObject(informant.getInformant3().getParam1().toByteArray());
          final Object param32 = kryoSerializer.asObject(informant.getInformant3().getParam2().toByteArray());
          final Object param33 = kryoSerializer.asObject(informant.getInformant3().getParam3().toByteArray());
          method.invoke(observerClassObject, param31, param32, param33);
          break;
        case 4:
          final Object param41 = kryoSerializer.asObject(informant.getInformant4().getParam1().toByteArray());
          final Object param42 = kryoSerializer.asObject(informant.getInformant4().getParam2().toByteArray());
          final Object param43 = kryoSerializer.asObject(informant.getInformant4().getParam3().toByteArray());
          final Object param44 = kryoSerializer.asObject(informant.getInformant4().getParam4().toByteArray());
          method.invoke(observerClassObject, param41, param42, param43, param44);
          break;
        default:
          throw new UnexpectedException();
      }
    } catch (Exception e) {
      log.error("Error in informing observer {} about message {}", observer, message);
    }
  }

  private Informant getInformant(Message message) {
    try {
      return Informant.parseFrom(message.getMessage().getData());
    } catch (InvalidProtocolBufferException e) {
      throw new InvalidRequestException(String.format("Invalid message for message id [%s]", message.getId()));
    }
  }
}
