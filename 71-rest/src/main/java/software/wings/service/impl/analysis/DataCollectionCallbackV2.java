package software.wings.service.impl.analysis;

import com.google.inject.Inject;

import io.harness.delegate.beans.ResponseData;
import io.harness.waiter.NotifyCallback;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.verification.CVTaskService;

import java.util.Map;

@Data
@Builder
@Slf4j
public class DataCollectionCallbackV2 implements NotifyCallback {
  @Inject private transient CVTaskService cvTaskService;

  private String cvTaskId;

  @Override
  public void notify(Map<String, ResponseData> response) {
    final DataCollectionTaskResult result = (DataCollectionTaskResult) response.values().iterator().next();
    cvTaskService.updateTaskStatus(cvTaskId, result);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {}
}
