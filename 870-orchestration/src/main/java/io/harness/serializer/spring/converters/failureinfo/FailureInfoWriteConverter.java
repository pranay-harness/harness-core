package io.harness.serializer.spring.converters.failureinfo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.execution.failure.FailureInfo;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class FailureInfoWriteConverter extends ProtoWriteConverter<FailureInfo> {}
