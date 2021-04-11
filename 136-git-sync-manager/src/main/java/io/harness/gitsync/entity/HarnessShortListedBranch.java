package io.harness.gitsync.entity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "HarnessShortListedBranchesKeys")
@Entity(value = "shortlistedBranches", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("shortlistedBranches")
@TypeAlias("io.harness.gitsync.entity.HarnessShortListedBranches")
@OwnedBy(DX)
public class HarnessShortListedBranch {
  @JsonIgnore @Id @org.mongodb.morphia.annotations.Id String id;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @JsonIgnore String accountIdentifier;
  @NotNull String yamlGitConfigId;
  @NotEmpty @JsonIgnore String branchName;
  @JsonIgnore @CreatedBy private EmbeddedUser createdBy;
  @JsonIgnore @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @JsonIgnore @Version Long version;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationId_projectId_yamlGitConfigId_branch_idx")
                 .unique(true)
                 .field(HarnessShortListedBranchesKeys.accountIdentifier)
                 .field(HarnessShortListedBranchesKeys.orgIdentifier)
                 .field(HarnessShortListedBranchesKeys.projectIdentifier)
                 .field(HarnessShortListedBranchesKeys.yamlGitConfigId)
                 .field(HarnessShortListedBranchesKeys.branchName)
                 .build())
        .build();
  }
}
