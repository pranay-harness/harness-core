package io.harness.gitsync.branching;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document("entityGitBranchMetadata")
@TypeAlias("io.harness.gitsync.beans.entityGitBranchMetadata")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "entityGitBranchMetadata", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "EntityGitBranchMetadataKeys")
@OwnedBy(DX)
// todo(abhinav): add indexes
public class EntityGitBranchMetadata {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String entityType;
  String branch;
  String yamlGitConfigId;
  String objectId;
  String orgIdentifier;
  String projectIdentifier;
  String entityFqn;
  String accountId;
  Boolean isDefault;
}
