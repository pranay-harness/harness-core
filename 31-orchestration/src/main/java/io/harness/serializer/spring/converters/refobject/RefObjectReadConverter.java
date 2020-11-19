package io.harness.serializer.spring.converters.refobject;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.refobjects.RefObject;

public class RefObjectReadConverter extends ProtoReadConverter<RefObject> {
  public RefObjectReadConverter() {
    super(RefObject.class);
  }
}
