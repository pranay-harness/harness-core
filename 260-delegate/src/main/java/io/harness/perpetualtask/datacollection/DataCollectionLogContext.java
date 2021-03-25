package io.harness.perpetualtask.datacollection;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.DataCollectionType;
import io.harness.logging.AutoLogContext;

import java.util.HashMap;
import java.util.Map;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class DataCollectionLogContext extends AutoLogContext {
  public static final String DATA_COLLECTION_WORKER_ID = "dataCollectionWorkerId";
  public static final String VERIFICATION_TYPE = "verificationType";

  private static Map<String, String> getContext(String dataCollectionWorkerId, DataCollectionType dataCollectionType) {
    Map<String, String> contextMap = new HashMap<>();
    if (hasSome(dataCollectionWorkerId)) {
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
