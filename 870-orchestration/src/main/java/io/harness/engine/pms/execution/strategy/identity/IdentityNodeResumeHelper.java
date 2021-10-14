package io.harness.engine.pms.execution.strategy.identity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.resume.publisher.NodeResumeEventPublisher;
import io.harness.execution.NodeExecution;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class IdentityNodeResumeHelper {
  @Inject private NodeResumeEventPublisher nodeResumeEventPublisher;

  public void resume(NodeExecution nodeExecution, Map<String, ByteString> responseMap, boolean isError) {
    nodeResumeEventPublisher.publishEvent(nodeExecution, responseMap, isError);
  }
}
