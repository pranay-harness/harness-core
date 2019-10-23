package software.wings.search.framework;

import lombok.Builder;
import lombok.Value;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.Queue;

@Value
@Builder
class ElasticsearchBulkSyncTaskResult {
  private boolean isSuccessful;
  private Queue<ChangeEvent<?>> changeEventsDuringBulkSync;
}
