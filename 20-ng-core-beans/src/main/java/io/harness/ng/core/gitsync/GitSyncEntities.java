package io.harness.ng.core.gitsync;

import io.harness.EntityType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GitSyncEntities {
  private List<GitSyncChangeSet> gitSyncChangeSets;
  private EntityType entityType;
}
