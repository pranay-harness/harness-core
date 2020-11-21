package io.harness.ng.core.gitsync;

import io.harness.EntityType;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitSyncEntities {
  private List<GitSyncChangeSet> gitSyncChangeSets;
  private EntityType entityType;
}
