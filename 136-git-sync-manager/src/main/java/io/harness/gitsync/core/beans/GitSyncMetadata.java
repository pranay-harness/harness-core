package io.harness.gitsync.core.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity(value = "gitSyncMetadataNG", noClassnameStored = true)
@Document("gitSyncMetadataNG")
@TypeAlias("io.harness.gitsync.core.beans.gitSyncMetadata")
public class GitSyncMetadata {
  String gitConnectorId;
  String repoName;
  String branchName;
  String yamlGitConfigId;
}
