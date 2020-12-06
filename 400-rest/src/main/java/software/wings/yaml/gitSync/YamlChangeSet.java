package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;

import software.wings.beans.Base;
import software.wings.beans.yaml.GitFileChange;

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

/**
 * @author bsollish 9/26/17
 */
@Data
@EqualsAndHashCode(callSuper = false)

@CdIndex(name = "searchIdx_1", fields = { @Field("accountId")
                                          , @Field("status"), @Field("retryCount") })
@CdIndex(name = "accountId_createdAt_index",
    fields = { @Field("accountId")
               , @Field(value = "createdAt", type = IndexType.DESC) })
@CdIndex(name = "accountId_status_gitToHarness_createdAt_index",
    fields =
    { @Field("accountId")
      , @Field(value = "status"), @Field(value = "gitToHarness"), @Field(value = "createdAt") })
@CdIndex(name = "accountId_queuekey_status_createdAt_index",
    fields =
    {
      @Field("accountId")
      , @Field(value = "queueKey"), @Field(value = "status"), @Field(value = "createdAt", type = IndexType.DESC)
    })
@FieldNameConstants(innerTypeName = "YamlChangeSetKeys")
@Entity(value = "yamlChangeSet")
@HarnessEntity(exportable = false)
public class YamlChangeSet extends Base {
  public static final String MAX_RETRY_COUNT_EXCEEDED_CODE = "MAX_RETRY_COUNT_EXCEEDED";
  public static final String MAX_QUEUE_DURATION_EXCEEDED_CODE = "MAX_QUEUE_DURATION_EXCEEDED";

  @NotEmpty private String accountId;
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
  @Default private Integer pushRetryCount = 0;
  private GitSyncMetadata gitSyncMetadata;

  public enum Status { QUEUED, RUNNING, FAILED, COMPLETED, SKIPPED }
  public static final List<Status> terminalStatusList =
      ImmutableList.of(Status.FAILED, Status.COMPLETED, Status.SKIPPED);

  @Builder
  public YamlChangeSet(String appId, String accountId, List<GitFileChange> gitFileChanges, Status status,
      boolean gitToHarness, boolean forcePush, long queuedOn, boolean fullSync, String parentYamlChangeSetId,
      GitWebhookRequestAttributes gitWebhookRequestAttributes, Integer retryCount, String messageCode, String queueKey,
      GitSyncMetadata gitSyncMetadata, Integer pushRetryCount) {
    this.appId = appId;
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
    this.pushRetryCount = pushRetryCount;
  }
}
