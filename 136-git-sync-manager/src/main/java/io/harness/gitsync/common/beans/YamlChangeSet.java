package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;
import io.harness.gitsync.core.beans.GitWebhookRequestAttributes;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
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
public class YamlChangeSet implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static final String MAX_RETRY_COUNT_EXCEEDED_CODE = "MAX_RETRY_COUNT_EXCEEDED";
  public static final String MAX_QUEUE_DURATION_EXCEEDED_CODE = "MAX_QUEUE_DURATION_EXCEEDED";

  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @Trimmed @NotEmpty @NotNull private String accountId;
  @FdIndex @NotNull private String status;
  @Builder.Default private long queuedOn = System.currentTimeMillis();
  // todo: replace/modify with whatever comes from webhook svc
  private GitWebhookRequestAttributes gitWebhookRequestAttributes;
  @Default private Integer retryCount = 0;
  @NotNull private String queueKey;
  @NotNull private YamlChangeSetEventType eventType;
  private String messageCode;
  private String repoUrl;
  private String branch;

  // Any special event metadata which has to go back from queue as is can be pushed in this interface.
  EventMetadata eventMetadata;

  @EqualsAndHashCode.Exclude @CreatedDate private long createdAt;
  @EqualsAndHashCode.Exclude @LastModifiedDate private long lastUpdatedAt;

  @Builder
  public YamlChangeSet(String uuid, String accountId, String status, long queuedOn,
      GitWebhookRequestAttributes gitWebhookRequestAttributes, Integer retryCount, String queueKey,
      YamlChangeSetEventType eventType, String messageCode, String repoUrl, String branch, EventMetadata eventMetadata,
      long createdAt, long lastUpdatedAt) {
    this.uuid = uuid;
    this.accountId = accountId;
    this.status = status;
    this.queuedOn = queuedOn;
    this.gitWebhookRequestAttributes = gitWebhookRequestAttributes;
    this.retryCount = retryCount;
    this.queueKey = queueKey;
    this.eventType = eventType;
    this.messageCode = messageCode;
    this.repoUrl = repoUrl;
    this.branch = branch;
    this.eventMetadata = eventMetadata;
    this.createdAt = createdAt;
    this.lastUpdatedAt = lastUpdatedAt;
  }
}
