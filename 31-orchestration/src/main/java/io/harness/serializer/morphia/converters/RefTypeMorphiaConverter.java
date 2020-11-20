package io.harness.serializer.morphia.converters;

import com.google.inject.Singleton;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.refobjects.RefType;

@Singleton
public class RefTypeMorphiaConverter extends ProtoMessageConverter<RefType> {
  public RefTypeMorphiaConverter() {
    super(RefType.class);
  }
}
