package io.harness.serializer.spring.converters.steps;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.steps.StepType;
import org.springframework.data.convert.WritingConverter;

import static io.harness.annotations.dev.HarnessTeam.CDC;

@OwnedBy(CDC)
@WritingConverter
public class StepTypeWriteConverter extends ProtoWriteConverter<StepType> {}
