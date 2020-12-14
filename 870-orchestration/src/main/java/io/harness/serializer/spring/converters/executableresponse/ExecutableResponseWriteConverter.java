package io.harness.serializer.spring.converters.executableresponse;

import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.contracts.execution.ExecutableResponse;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@Singleton
@WritingConverter
public class ExecutableResponseWriteConverter extends ProtoWriteConverter<ExecutableResponse> {}
