package io.harness.ng.core.gitsync;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitSyncChanges {
  private List<GitSyncEntities> gitSyncEntitiesList;
}
