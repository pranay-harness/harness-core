package io.harness.gitsync.gitfileactivity.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.ng.core.OrganizationAccess;
import io.harness.ng.core.ProjectAccess;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@AllArgsConstructor
@Entity(value = "gitFileActivitySummaryNG", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "GitFileActivitySummaryKeys")
@HarnessEntity(exportable = true)
@Document("gitFileActivitySummaryNG")
@TypeAlias("io.harness.gitsync.gitfileactivity.beans.gitFileActivitySummary")
public class GitFileActivitySummary implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware,
                                               AccountAccess, OrganizationAccess, ProjectAccess {
  @org.springframework.data.annotation.Id @org.mongodb.morphia.annotations.Id private String uuid;
  private String accountId;
  private String organizationId;
  private String projectId;
  private String commitId;
  private String branchName;
  private String repo;
  private String gitConnectorId;
  private long createdAt;
  private String commitMessage;
  private long lastUpdatedAt;
  private Boolean gitToHarness;
  private GitCommit.Status status;
  private GitFileProcessingSummary fileProcessingSummary;
}
