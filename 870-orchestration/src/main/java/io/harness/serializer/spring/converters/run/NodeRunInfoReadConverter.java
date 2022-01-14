package io.harness.serializer.spring.converters.run;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.serializer.spring.ProtoReadConverter;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class NodeRunInfoReadConverter extends ProtoReadConverter<NodeRunInfo> {
  public NodeRunInfoReadConverter() {
    super(NodeRunInfo.class);
  }
}
