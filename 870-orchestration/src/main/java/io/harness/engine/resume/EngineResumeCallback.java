package io.harness.engine.resume;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class EngineResumeCallback implements NotifyCallback {
  @Inject OrchestrationEngine orchestrationEngine;
  @Inject ResponseDataMapper responseDataMapper;

  String nodeExecutionId;

  @Override
  public void notify(Map<String, ResponseData> response) {
    Map<String, ByteString> byteStringMap = responseDataMapper.toResponseDataProto(response);
    orchestrationEngine.resume(nodeExecutionId, byteStringMap, false);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    Map<String, ByteString> byteStringMap = responseDataMapper.toResponseDataProto(response);
    orchestrationEngine.resume(nodeExecutionId, byteStringMap, true);
  }
}
