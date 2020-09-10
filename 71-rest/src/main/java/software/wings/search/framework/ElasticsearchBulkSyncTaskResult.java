package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.Queue;

@OwnedBy(PL)
@Value
@Builder
class ElasticsearchBulkSyncTaskResult {
  private boolean isSuccessful;
  private Queue<ChangeEvent<?>> changeEventsDuringBulkSync;
}
