package io.harness.serializer.spring.converters.refobject;

import com.google.inject.Singleton;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.refobjects.RefObject;
import org.springframework.data.convert.ReadingConverter;

@Singleton
@ReadingConverter
public class RefObjectReadConverter extends ProtoReadConverter<RefObject> {
  public RefObjectReadConverter() {
    super(RefObject.class);
  }
}
