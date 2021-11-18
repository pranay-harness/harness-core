package io.harness.observer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.serializer.KryoSerializer;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.DEL)
@Slf4j
public abstract class AbstractRemoteInformer {
  private final KryoSerializer kryoSerializer;
  private final Producer eventProducer;

  public AbstractRemoteInformer(KryoSerializer kryoSerializer, Producer eventProducer) {
    this.kryoSerializer = kryoSerializer;
    this.eventProducer = eventProducer;
  }

  public void fireInform(String methodName, Class<?> observerClass, Class<?> subjectClass, Object... param) {
    final int length = param.length;
    Informant.Builder informantBuilder = Informant.newBuilder().setMethodName(methodName);
    switch (length) {
      case 0:
        informantBuilder.setInformant0(Informant0.newBuilder().build());
        break;
      case 1:
        informantBuilder.setInformant1(Informant1.newBuilder().setParam1(getByteString(param[0])).build());
        break;
      case 2:
        informantBuilder.setInformant2(
            Informant2.newBuilder().setParam1(getByteString(param[0])).setParam2(getByteString(param[1])).build());
        break;
      case 3:
        informantBuilder.setInformant3(Informant3.newBuilder()
                                           .setParam1(getByteString(param[0]))
                                           .setParam2(getByteString(param[1]))
                                           .setParam3(getByteString(param[3]))
                                           .build());
        break;
      case 4:
        informantBuilder.setInformant4(Informant4.newBuilder()
                                           .setParam1(getByteString(param[0]))
                                           .setParam2(getByteString(param[1]))
                                           .setParam3(getByteString(param[2]))
                                           .setParam4(getByteString(param[3]))
                                           .build());
        break;
      default:
        throw new UnsupportedOperationException("Only 4 params supported in subject observers");
    }
    try {
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of(RemoteObserverConstants.SUBJECT_CLASS_NAME, subjectClass.getName(),
                  RemoteObserverConstants.OBSERVER_CLASS_NAME, observerClass.getName()))
              .setData(informantBuilder.build().toByteString())
              .build());
    } catch (Exception e) {
      log.error("Exception in producing event for clazz [{}], method name: [{}]", subjectClass, methodName);
    }
  }

  private ByteString getByteString(Object object) {
    final byte[] bytes = kryoSerializer.asBytes(object);
    return ByteString.copyFrom(bytes);
  }
}
