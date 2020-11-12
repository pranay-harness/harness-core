package io.harness.perpetualtask.datacollection;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.DataCollectionType;
import io.harness.logging.AutoLogContext;

import java.util.HashMap;
import java.util.Map;

public class DataCollectionLogContext extends AutoLogContext {
  public static final String DATA_COLLECTION_WORKER_ID = "dataCollectionWorkerId";
  public static final String VERIFICATION_TYPE = "verificationType";

  private static Map<String, String> getContext(String dataCollectionWorkerId, DataCollectionType dataCollectionType) {
    Map<String, String> contextMap = new HashMap<>();
    if (isNotEmpty(dataCollectionWorkerId)) {
      contextMap.put(DATA_COLLECTION_WORKER_ID, dataCollectionWorkerId);
    }
    if (dataCollectionType != null) {
      contextMap.put(VERIFICATION_TYPE, dataCollectionType.name());
    }
    return contextMap;
  }

  public DataCollectionLogContext(
      String dataCollectionWorkerId, DataCollectionType dataCollectionType, OverrideBehavior behavior) {
    super(getContext(dataCollectionWorkerId, dataCollectionType), behavior);
  }
}
