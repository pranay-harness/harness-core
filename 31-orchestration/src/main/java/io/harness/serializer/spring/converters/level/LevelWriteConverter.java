package io.harness.serializer.spring.converters.level;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.ambiance.Level;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@WritingConverter
public class LevelWriteConverter extends ProtoWriteConverter<Level> {}
