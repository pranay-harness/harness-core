package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.encryption.Scope;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.core.beans.GitSyncMetadata;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.mongo.index.FdIndex;
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
@Document("yamlChangeSetNG")
@TypeAlias("io.harness.gitsync.common.beans.yamlChangeSet")
@Entity(value = "yamlChangeSetNG", noClassnameStored = true)
@OwnedBy(DX)
public class YamlChangeSet
    implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, UpdatedAtAware, UpdatedByAware {
  public static final String MAX_RETRY_COUNT_EXCEEDED_CODE = "MAX_RETRY_COUNT_EXCEEDED";
  public static final String MAX_QUEUE_DURATION_EXCEEDED_CODE = "MAX_QUEUE_DURATION_EXCEEDED";

  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @Trimmed @NotEmpty private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  @FdIndex @NotNull private String status;
  private Scope scope;

  @Default private Integer retryCount = 0;
  private String messageCode;
  private String queueKey;
  private GitSyncMetadata gitSyncMetadata;

  @NotNull private List<GitFileChange> gitFileChanges = new ArrayList<>();
  private boolean forcePush;
  private long queuedOn = System.currentTimeMillis();
  private boolean fullSync;
  private GitWebhookRequestAttributes gitWebhookRequestAttributes;

  public enum Status { QUEUED, RUNNING, FAILED, COMPLETED, SKIPPED }

  public static final List<Status> terminalStatusList =
      ImmutableList.of(Status.FAILED, Status.COMPLETED, Status.SKIPPED);

  @CreatedBy private EmbeddedUser createdBy;
  @CreatedDate private long createdAt;
  @LastModifiedBy private EmbeddedUser lastUpdatedBy;
  @LastModifiedDate private long lastUpdatedAt;

  @Builder
  public YamlChangeSet(String uuid, String accountIdentifier, List<GitFileChange> gitFileChanges, String status,
      boolean forcePush, long queuedOn, boolean fullSync, GitWebhookRequestAttributes gitWebhookRequestAttributes,
      Integer retryCount, String messageCode, String queueKey, GitSyncMetadata gitSyncMetadata, String orgIdentifier,
      String projectIdentifier, Scope scope) {
    this.uuid = uuid;
    this.accountIdentifier = accountIdentifier;
    this.gitFileChanges = gitFileChanges;
    this.status = status;
    this.forcePush = forcePush;
    this.queuedOn = queuedOn;
    this.fullSync = fullSync;
    this.gitWebhookRequestAttributes = gitWebhookRequestAttributes;
    this.retryCount = retryCount;
    this.messageCode = messageCode;
    this.queueKey = queueKey;
    this.gitSyncMetadata = gitSyncMetadata;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.scope = scope;
  }
}
