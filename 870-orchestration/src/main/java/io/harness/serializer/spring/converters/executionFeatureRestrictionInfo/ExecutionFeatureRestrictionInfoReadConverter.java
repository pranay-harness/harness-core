package io.harness.serializer.spring.converters.executionFeatureRestrictionInfo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionFeatureRestrictionInfo;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(PIPELINE)
@Singleton
@ReadingConverter
public class ExecutionFeatureRestrictionInfoReadConverter extends ProtoReadConverter<ExecutionFeatureRestrictionInfo> {
  public ExecutionFeatureRestrictionInfoReadConverter() {
    super(ExecutionFeatureRestrictionInfo.class);
  }
}
