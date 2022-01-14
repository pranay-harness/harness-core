package io.harness.serializer.spring.converters.refobject;

import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@Singleton
@ReadingConverter
public class RefObjectReadConverter extends ProtoReadConverter<RefObject> {
  public RefObjectReadConverter() {
    super(RefObject.class);
  }
}
