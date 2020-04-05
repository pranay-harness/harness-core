package software.wings.yaml.gitSync;

import io.harness.annotation.HarnessEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Base;
import software.wings.beans.yaml.GitFileChange;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * @author bsollish 9/26/17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Indexes({
  @Index(fields = { @Field("accountId")
                    , @Field("status") }, options = @IndexOptions(name = "searchIdx"))
  , @Index(fields = {
    @Field("accountId"), @Field(value = "createdAt", type = IndexType.DESC)
  }, options = @IndexOptions(name = "accountId_createdAt_index", background = true)), @Index(fields = {
    @Field("accountId"), @Field(value = "status"), @Field(value = "gitToHarness"), @Field(value = "createdAt")
  }, options = @IndexOptions(name = "accountId_status_gitToHarness_createdAt_index", background = true))
})
@FieldNameConstants(innerTypeName = "YamlChangeSetKeys")
@Entity(value = "yamlChangeSet")
@HarnessEntity(exportable = false)
public class YamlChangeSet extends Base {
  @NotEmpty private String accountId;
  @NotNull private List<GitFileChange> gitFileChanges = new ArrayList<>();
  @Indexed @NotNull private Status status;
  private boolean gitToHarness;
  private boolean forcePush;
  private long queuedOn = System.currentTimeMillis();
  private boolean fullSync;
  private String parentYamlChangeSetId;
  private GitWebhookRequestAttributes gitWebhookRequestAttributes;

  public enum Status { QUEUED, RUNNING, FAILED, COMPLETED, SKIPPED }

  @Builder
  public YamlChangeSet(String appId, String accountId, List<GitFileChange> gitFileChanges, Status status,
      boolean gitToHarness, boolean forcePush, long queuedOn, boolean fullSync, String parentYamlChangeSetId,
      GitWebhookRequestAttributes gitWebhookRequestAttributes) {
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
  }
}
