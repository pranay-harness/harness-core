package io.harness.engine.pms.resume.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;

import com.google.protobuf.ByteString;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeResumeEventPublisher {
  void publishEvent(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError);

  void publishEventForIdentityNode(
      NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError, String serviceName);
}
