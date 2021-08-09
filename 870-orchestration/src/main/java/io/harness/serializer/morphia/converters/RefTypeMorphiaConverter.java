package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.contracts.refobjects.RefType;

import com.google.inject.Singleton;

@Singleton
public class RefTypeMorphiaConverter extends ProtoMessageConverter<RefType> {
  public RefTypeMorphiaConverter() {
    super(RefType.class);
  }
}
