package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.Queue;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
class ElasticsearchBulkSyncTaskResult {
  private boolean isSuccessful;
  private Queue<ChangeEvent<?>> changeEventsDuringBulkSync;
}
