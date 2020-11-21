package io.harness.gitsync.common.beans;

import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.encryption.Scope;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.core.beans.GitSyncMetadata;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.core.OrganizationAccess;
import io.harness.ng.core.ProjectAccess;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
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
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "YamlChangeSetKeys")
@Document("yamlChangeSet")
@TypeAlias("io.harness.gitsync.common.beans.yamlChangeSet")
@Entity(value = "yamlChangeSet", noClassnameStored = true)
public class YamlChangeSet implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware,
                                      UpdatedByAware, AccountAccess, OrganizationAccess, ProjectAccess {
  public static final String MAX_RETRY_COUNT_EXCEEDED_CODE = "MAX_RETRY_COUNT_EXCEEDED";
  public static final String MAX_QUEUE_DURATION_EXCEEDED_CODE = "MAX_QUEUE_DURATION_EXCEEDED";
  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @Trimmed @NotEmpty private String accountId;
  @NotNull private List<GitFileChange> gitFileChanges = new ArrayList<>();
  @FdIndex @NotNull private Status status;
  private boolean gitToHarness;
  private boolean forcePush;
  private long queuedOn = System.currentTimeMillis();
  private boolean fullSync;
  private String parentYamlChangeSetId;
  private GitWebhookRequestAttributes gitWebhookRequestAttributes;
  @Default private Integer retryCount = 0;
  private String messageCode;
  private String queueKey;
  private GitSyncMetadata gitSyncMetadata;
  private String organizationId;
  private String projectId;
  private Scope scope;

  public enum Status { QUEUED, RUNNING, FAILED, COMPLETED, SKIPPED }

  public static final List<Status> terminalStatusList =
      ImmutableList.of(Status.FAILED, Status.COMPLETED, Status.SKIPPED);

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  @Builder
  public YamlChangeSet(String organizationId, String projectId, String accountId, List<GitFileChange> gitFileChanges,
      Status status, boolean gitToHarness, boolean forcePush, long queuedOn, boolean fullSync,
      String parentYamlChangeSetId, GitWebhookRequestAttributes gitWebhookRequestAttributes, Integer retryCount,
      String messageCode, String queueKey, GitSyncMetadata gitSyncMetadata, Scope scope) {
    this.accountId = accountId;
    this.gitFileChanges = gitFileChanges;
    this.status = status;
    this.gitToHarness = gitToHarness;
    this.forcePush = forcePush;
    this.queuedOn = queuedOn;
    this.fullSync = fullSync;
    this.parentYamlChangeSetId = parentYamlChangeSetId;
    this.gitWebhookRequestAttributes = gitWebhookRequestAttributes;
    this.retryCount = retryCount;
    this.messageCode = messageCode;
    this.queueKey = queueKey;
    this.gitSyncMetadata = gitSyncMetadata;
    this.organizationId = organizationId;
    this.projectId = projectId;
    this.scope = scope;
  }
}
