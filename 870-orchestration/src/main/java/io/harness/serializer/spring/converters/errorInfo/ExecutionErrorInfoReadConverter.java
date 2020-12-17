package io.harness.serializer.spring.converters.errorInfo;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.steps.StepType;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class ExecutionErrorInfoReadConverter extends ProtoReadConverter<ExecutionErrorInfo> {
  public ExecutionErrorInfoReadConverter() {
    super(ExecutionErrorInfo.class);
  }
}
