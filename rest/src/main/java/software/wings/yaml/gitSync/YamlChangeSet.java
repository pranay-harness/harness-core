package software.wings.yaml.gitSync;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.beans.yaml.GitFileChange;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * @author bsollish 9/26/17
 */
@Entity(value = "yamlChangeSet")
@Data
@EqualsAndHashCode(callSuper = false)
public class YamlChangeSet extends Base {
  @Indexed @NotEmpty private String accountId;
  @NotNull private List<GitFileChange> gitFileChanges = new ArrayList<>();
  @Indexed @NotNull private Status status;
  private boolean gitToHarness;
  private boolean forcePush;
  @Indexed private long queuedOn = System.currentTimeMillis();
  private boolean fullSync;

  public enum Status { QUEUED, RUNNING, FAILED, COMPLETED, SKIPPED }

  @Builder
  public YamlChangeSet(String appId, String accountId, List<GitFileChange> gitFileChanges, Status status,
      boolean gitToHarness, boolean forcePush, long queuedOn, boolean fullSync) {
    this.appId = appId;
    this.accountId = accountId;
    this.gitFileChanges = gitFileChanges;
    this.status = status;
    this.gitToHarness = gitToHarness;
    this.forcePush = forcePush;
    this.queuedOn = queuedOn;
    this.fullSync = fullSync;
  }
}
