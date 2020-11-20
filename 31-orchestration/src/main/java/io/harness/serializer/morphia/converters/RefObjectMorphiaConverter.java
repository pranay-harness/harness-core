package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.refobjects.RefObject;

@Singleton
public class RefObjectMorphiaConverter extends ProtoMessageConverter<RefObject> {
  public RefObjectMorphiaConverter() {
    super(RefObject.class);
  }
}
