package io.harness.serializer.spring.converters.level;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.ambiance.Level;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class LevelReadConverter extends ProtoReadConverter<Level> {
  public LevelReadConverter() {
    super(Level.class);
  }
}