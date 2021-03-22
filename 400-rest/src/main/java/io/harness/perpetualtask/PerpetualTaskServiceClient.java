package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;

import com.google.protobuf.Message;

/**
 * Used on the manager side to handle CRUD of a specific type of perpetual tasks.
 */
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@Deprecated
public interface PerpetualTaskServiceClient {
  Message getTaskParams(PerpetualTaskClientContext clientContext);
  DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId);
}
