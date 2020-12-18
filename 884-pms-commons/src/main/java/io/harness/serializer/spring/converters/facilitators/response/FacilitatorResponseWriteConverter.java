package io.harness.serializer.spring.converters.facilitators.response;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class FacilitatorResponseWriteConverter extends ProtoWriteConverter<FacilitatorResponseProto> {}
