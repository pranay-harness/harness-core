package io.harness.serializer.spring.converters.advisertype;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.advisers.AdviserType;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@WritingConverter
public class AdviserTypeWriteConverter extends ProtoWriteConverter<AdviserType> {}
