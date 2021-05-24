package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSetStatus;
import io.harness.gitsync.core.runnable.ChangeSetGroupingKey;
import io.harness.validation.Create;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.query.Criteria;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

@OwnedBy(DX)
public interface YamlChangeSetService {
  @ValidationGroups(Create.class) YamlChangeSet save(@Valid YamlChangeSet yamlChangeSet);

  Optional<YamlChangeSet> get(@NotEmpty String accountId, @NotEmpty String changeSetId);

  Optional<YamlChangeSet> peekQueueHead(String accountId, String queueKey, YamlChangeSetStatus yamlChangeSetStatus);

  Optional<YamlChangeSet> getOldestChangeSet(
      String accountId, String queueKey, YamlChangeSetStatus yamlChangeSetStatus);

  boolean changeSetExistsFoQueueKey(
      String accountId, String queueKey, @Size(min = 1) List<YamlChangeSetStatus> yamlChangeSetStatuses);

  int countByAccountIdAndStatus(String accountId, List<YamlChangeSetStatus> yamlChangeSetStatuses);

  boolean updateStatus(
      @NotEmpty String accountId, @NotEmpty String changeSetId, @NotNull YamlChangeSetStatus newStatus);

  boolean updateStatusForGivenYamlChangeSets(String accountId, YamlChangeSetStatus newStatus,
      List<YamlChangeSetStatus> currentStatuses, List<String> yamlChangeSetIds);

  void markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(String accountId, int maxRetryCount);

  boolean updateStatusAndIncrementRetryCountForYamlChangeSets(String accountId, YamlChangeSetStatus newStatus,
      List<YamlChangeSetStatus> currentStatus, List<String> yamlChangeSetIds);

  List<YamlChangeSet> findByAccountIdsStatusLastUpdatedAtLessThan(
      List<String> runningAccountIdList, List<YamlChangeSetStatus> yamlChangeSetStatuses, long timeout);

  List<String> findDistinctAccountIdsByStatus(List<YamlChangeSetStatus> status);

  UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
      YamlChangeSetStatus oldStatus, YamlChangeSetStatus newStatus, long timeout, String messageCode);

  Set<ChangeSetGroupingKey> getChangesetGroupingKeys(Criteria criteria);

  Optional<YamlChangeSet> getOldestGitToHarnessChangeSet(String accountId, String queueKey);

  List<YamlChangeSet> list(String queueKey, String accountId, YamlChangeSetStatus status);
}
