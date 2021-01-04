package io.harness.serializer.spring.converters.reftype;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.serializer.spring.ProtoReadConverter;

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
