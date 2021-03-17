package io.harness.serializer.spring.converters.logging;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.UnitProgress;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class UnitProgressReadConverter extends ProtoReadConverter<UnitProgress> {
  public UnitProgressReadConverter() {
    super(UnitProgress.class);
  }
}
