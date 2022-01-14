package io.harness.engine.pms.resume.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.protobuf.ByteString;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface NodeResumeEventPublisher {
  void publishEvent(ResumeMetadata resumeMetadata, Map<String, ByteString> responseMap, boolean isError);

  void publishEventForIdentityNode(
      ResumeMetadata resumeMetadata, Map<String, ByteString> responseMap, boolean isError, String serviceName);
}
