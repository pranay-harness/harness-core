package io.harness.serializer.spring.converters.reftype;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.refobjects.RefType;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class RefTypeReadConverter extends ProtoReadConverter<RefType> {
  public RefTypeReadConverter() {
    super(RefType.class);
  }
}
