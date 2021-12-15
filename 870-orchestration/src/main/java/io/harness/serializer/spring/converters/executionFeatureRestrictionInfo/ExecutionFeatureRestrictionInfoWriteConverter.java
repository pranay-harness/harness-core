package io.harness.serializer.spring.converters.executionFeatureRestrictionInfo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.ExecutionFeatureRestrictionInfo;
import io.harness.serializer.spring.ProtoWriteConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(PIPELINE)
@Singleton
@WritingConverter
public class ExecutionFeatureRestrictionInfoWriteConverter
    extends ProtoWriteConverter<ExecutionFeatureRestrictionInfo> {}
