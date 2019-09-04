package software.wings.delegatetasks.cv;

import software.wings.service.impl.analysis.DataCollectionInfoV2;

public interface DataCollector<T extends DataCollectionInfoV2> {
  void init(DataCollectionCallback dataCollectionCallback, T dataCollectionInfo) throws DataCollectionException;
}
