package io.harness.serializer.morphia.converters;

import io.harness.persistence.converters.ProtoMessageConverter;
import io.harness.pms.refobjects.RefType;

public class RefTypeMorphiaConverter extends ProtoMessageConverter<RefType> {
  public RefTypeMorphiaConverter() {
    super(RefType.class);
  }
}
