package software.wings.service.intfc.yaml;

import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.yaml.GitFileChange;
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

  void populateGitSyncMetadata(YamlChangeSet yamlChangeSet);

  /**
   * Get yaml change set.
   *
   * @param accountId   the account id
   * @param changeSetId the change set id
   * @return the yaml change set
   */
  YamlChangeSet get(@NotEmpty String accountId, @NotEmpty String changeSetId);

  /**
   * Update status boolean.
   *
   * @param accountId   the account id
   * @param changeSetId the change set id
   * @param newStatus   the new status
   * @return the boolean
   */
  boolean updateStatus(@NotEmpty String accountId, @NotEmpty String changeSetId, @NotNull Status newStatus);

  boolean updateStatusForGivenYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatuses, List<String> yamlChangeSetIds);

  /**
   * Delete change set boolean.
   *
   * @param accountId   the account id
   * @param changeSetId the change set id
   * @return the boolean
   */
  boolean deleteChangeSet(@NotEmpty String accountId, @NotEmpty String changeSetId);

  <T> YamlChangeSet saveChangeSet(String accountId, List<GitFileChange> changeSet, T entity);

  void markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(String accountId);

  boolean updateStatusAndIncrementRetryCountForYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatus, List<String> yamlChangeSetIds);

  boolean updateStatusForYamlChangeSets(String accountId, Status newStatus, Status currentStatus);

  YamlChangeSet getQueuedChangeSetForWaitingQueueKey(
      String accountId, String queueKey, int maxRunningChangesetsForAccount);

  List<YamlChangeSet> getChangeSetsToBeMarkedSkipped(String accountId);

  void deleteChangeSets(
      String accountId, Status[] statuses, Integer maxDeleteCount, String batchSize, int retentionPeriodInDays);

  List<YamlChangeSet> getChangeSetsWithStatus(String accountId, String appId, YamlGitConfig yamlGitConfig,
      int displayCount, List<YamlChangeSet.Status> statuses);
}
