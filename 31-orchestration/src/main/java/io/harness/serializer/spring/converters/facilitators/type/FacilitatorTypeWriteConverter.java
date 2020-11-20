package io.harness.serializer.spring.converters.facilitators.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.facilitators.FacilitatorType;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class FacilitatorTypeWriteConverter extends ProtoWriteConverter<FacilitatorType> {}
