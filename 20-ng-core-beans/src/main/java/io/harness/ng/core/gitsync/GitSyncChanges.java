package io.harness.ng.core.gitsync;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GitSyncChanges {
  private List<GitSyncEntities> gitSyncEntitiesList;
}
