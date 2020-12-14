package io.harness.serializer.spring.converters.plannode;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoWriteConverter;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.PlanNodeProto;

import com.google.inject.Singleton;
import org.springframework.data.convert.WritingConverter;

@OwnedBy(CDC)
@Singleton
@WritingConverter
public class PlanNodeProtoWriteConverter extends ProtoWriteConverter<PlanNodeProto> {}
