package io.harness.pms.mongo;

import io.harness.orchestration.persistence.ProtoReadConverter;
import io.harness.pms.execution.NodeExecution;

import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class NodeExecutionReadConverter extends ProtoReadConverter<NodeExecution> {
  public NodeExecutionReadConverter() {
    super(NodeExecution.class);
  }
}
