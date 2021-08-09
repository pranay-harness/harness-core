package io.harness.serializer.spring.converters.errorInfo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.serializer.spring.ProtoWriteConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class ExecutionErrorInfoWriteConverter extends ProtoWriteConverter<ExecutionErrorInfo> {}
