package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.yaml.GitFileChange;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 10/31/17.
 */
public interface YamlChangeSetService {
  /**
   * Save yaml change set.
   *
   * @param yamlChangeSet the yaml change set
   * @return the yaml change set
   */
  @ValidationGroups(Create.class) YamlChangeSet save(@Valid YamlChangeSet yamlChangeSet);

  /**
   * Update.
   *
   * @param yamlChangeSet the yaml change set
   */
  @ValidationGroups(Update.class) void update(@Valid YamlChangeSet yamlChangeSet);

  /**
   * Get yaml change set.
   *
   * @param accountId   the account id
   * @param changeSetId the change set id
   * @return the yaml change set
   */
  YamlChangeSet get(@NotEmpty String accountId, @NotEmpty String changeSetId);

  /**
   * Gets queued change set.
   *
   * @param accountId the account id
   * @return the queued change set
   */
  List<YamlChangeSet> getQueuedChangeSet(String accountId);

  /**
   * Update status boolean.
   *
   * @param accountId   the account id
   * @param changeSetId the change set id
   * @param newStatus   the new status
   * @return the boolean
   */
  boolean updateStatus(@NotEmpty String accountId, @NotEmpty String changeSetId, @NotNull Status newStatus);

  /**
   * Delete change set boolean.
   *
   * @param accountId   the account id
   * @param changeSetId the change set id
   * @return the boolean
   */
  boolean deleteChangeSet(@NotEmpty String accountId, @NotEmpty String changeSetId);

  /**
   * Queue change set.
   *
   * @param yamlGitConfig       the yamlGitConfig
   * @param changeSet the change set
   */
  void saveChangeSet(YamlGitConfig yamlGitConfig, List<GitFileChange> changeSet);

  boolean updateStatusForGivenYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatus, List<String> yamlChangeSetIds);

  boolean updateStatusForYamlChangeSets(String accountId, Status newStatus, Status currentStatus);

  List<YamlChangeSet> getChangeSetsToSync(String accountId);

  List<YamlChangeSet> getChangeSetsToBeMarkedSkipped(String accountId);

  void deleteChangeSets(
      String accountId, Status[] statuses, Integer maxDeleteCount, String batchSize, int retentionPeriodInDays);
}
