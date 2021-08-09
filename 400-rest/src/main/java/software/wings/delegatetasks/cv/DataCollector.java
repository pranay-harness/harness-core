package software.wings.delegatetasks.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.DataCollectionInfoV2;
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface DataCollector<T extends DataCollectionInfoV2> {
  void init(DataCollectionExecutionContext dataCollectionExecutionContext, T dataCollectionInfo)
      throws DataCollectionException;
  int getHostBatchSize();
}
