package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.NGAccess;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;

@Data
@FieldNameConstants(innerTypeName = "GitSyncableEntityKeys")
@OwnedBy(DX)
@NoArgsConstructor
@AllArgsConstructor
public abstract class GitSyncableEntity implements NGAccess {
  @Id @org.mongodb.morphia.annotations.Id String id;
  String objectIdOfYaml;
  Boolean isFromDefaultBranch;
  String yamlGitConfigRef;
  transient String branch;
}
