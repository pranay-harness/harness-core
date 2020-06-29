package software.wings.service.intfc;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;

public interface DelegateSyncService extends Runnable { <T extends ResponseData> T waitForTask(DelegateTask task); }
