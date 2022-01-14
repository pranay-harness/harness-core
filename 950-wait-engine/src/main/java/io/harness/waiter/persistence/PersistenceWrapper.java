package io.harness.waiter.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.waiter.ProcessedMessageResponse;
import io.harness.waiter.ProgressUpdate;
import io.harness.waiter.WaitEngineEntity;
import io.harness.waiter.WaitInstance;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PersistenceWrapper {
  String save(WaitEngineEntity entity);

  void delete(WaitEngineEntity entity);

  void deleteWaitInstance(WaitInstance entity);

  WaitInstance modifyAndFetchWaitInstance(String waitingOnCorrelationId);

  WaitInstance modifyAndFetchWaitInstanceForExistingResponse(String waitInstanceId, List<String> notifyResponseIds);

  WaitInstance fetchForProcessingWaitInstance(String waitInstanceId, long now);

  ProgressUpdate fetchForProcessingProgressUpdate(Set<String> busyCorrelationIds, long now);

  ProcessedMessageResponse processMessage(WaitInstance waitInstance);

  List<WaitInstance> fetchWaitInstances(String correlationId);

  List<String> fetchNotifyResponseKeys(long limit);

  void deleteNotifyResponses(List<String> responseIds);

  String saveWithTimeout(WaitInstance build, Duration timeout);
}
