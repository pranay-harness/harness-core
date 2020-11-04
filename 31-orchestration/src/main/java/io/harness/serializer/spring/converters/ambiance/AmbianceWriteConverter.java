package io.harness.serializer.spring.converters.ambiance;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.ambiance.Ambiance;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@WritingConverter
public class AmbianceWriteConverter extends ProtoWriteConverter<Ambiance> {}
