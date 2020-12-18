package io.harness.serializer.spring.converters.nodeexecution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.contracts.execution.NodeExecutionProto;

import com.google.inject.Singleton;
import org.springframework.data.convert.ReadingConverter;

@OwnedBy(CDC)
@Singleton
@ReadingConverter
public class NodeExecutionReadConverter extends ProtoReadConverter<NodeExecutionProto> {
  public NodeExecutionReadConverter() {
    super(NodeExecutionProto.class);
  }
}
