package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.refobjects.RefObject;

public class RefObjectMorphiaConverter extends ProtoMessageConverter<RefObject> {
  public RefObjectMorphiaConverter() {
    super(RefObject.class);
  }
}
