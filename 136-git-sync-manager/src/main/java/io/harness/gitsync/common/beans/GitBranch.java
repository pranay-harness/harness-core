package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "GitBranchKeys")
@Document("gitBranches")
@TypeAlias("io.harness.gitsync.common.beans.GitBranch")
@Entity(value = "gitBranches", noClassnameStored = true)
@StoreIn(DbAliases.NG_MANAGER)
@Persistent
@OwnedBy(DX)
public class GitBranch {
  @JsonIgnore @Id @org.mongodb.morphia.annotations.Id String uuid;
  String orgIdentifier;
  String projectIdentifier;
  @NotNull String accountIdentifier;
  @NotNull String yamlGitConfigIdentifier;
  @NotEmpty String branchName;
  @NotEmpty BranchSyncStatus branchSyncStatus;
  @CreatedBy private EmbeddedUser createdBy;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationId_projectId_yamlGitConfigId_branch_idx")
                 .unique(true)
                 .field(GitBranchKeys.accountIdentifier)
                 .field(GitBranchKeys.orgIdentifier)
                 .field(GitBranchKeys.projectIdentifier)
                 .field(GitBranchKeys.yamlGitConfigIdentifier)
                 .field(GitBranchKeys.branchName)
                 .build())
        .build();
  }
}
