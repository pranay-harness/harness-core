package io.harness.serializer.spring.converters.refobject;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.refobjects.RefObject;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@Singleton
@ReadingConverter
public class RefObjectReadConverter extends ProtoReadConverter<RefObject> {
  public RefObjectReadConverter() {
    super(RefObject.class);
  }
}
