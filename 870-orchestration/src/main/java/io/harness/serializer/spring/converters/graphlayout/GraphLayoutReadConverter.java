package io.harness.serializer.spring.converters.graphlayout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.plan.GraphLayoutNode;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class GraphLayoutReadConverter extends ProtoReadConverter<GraphLayoutNode> {
  public GraphLayoutReadConverter() {
    super(GraphLayoutNode.class);
  }
}