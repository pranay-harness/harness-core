package io.harness.serializer.spring.converters.plannode;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.PlanNodeProto;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class PlanNodeProtoReadConverter extends ProtoReadConverter<PlanNodeProto> {
  public PlanNodeProtoReadConverter() {
    super(PlanNodeProto.class);
  }
}
