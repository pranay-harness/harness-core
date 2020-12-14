package io.harness.serializer.spring.converters.executableresponse;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.execution.ExecutableResponse;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@Singleton
@ReadingConverter
public class ExecutableResponseReadConverter extends ProtoReadConverter<ExecutableResponse> {
  public ExecutableResponseReadConverter() {
    super(ExecutableResponse.class);
  }
}
