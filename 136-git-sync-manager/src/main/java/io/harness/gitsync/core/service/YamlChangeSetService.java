package io.harness.gitsync.core.service;

import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.beans.YamlChangeSet.Status;
import io.harness.validation.Create;
import io.harness.validation.Update;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

public interface YamlChangeSetService {
  @ValidationGroups(Create.class) YamlChangeSet save(@Valid YamlChangeSet yamlChangeSet);

  @ValidationGroups(Update.class) YamlChangeSet update(@Valid YamlChangeSet yamlChangeSet);

  void populateGitSyncMetadata(YamlChangeSet yamlChangeSet);

  Optional<YamlChangeSet> get(@NotEmpty String accountId, @NotEmpty String changeSetId);

  boolean updateStatus(@NotEmpty String accountId, @NotEmpty String changeSetId, @NotNull Status newStatus);

  boolean updateStatusForGivenYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatuses, List<String> yamlChangeSetIds);

  void markQueuedYamlChangeSetsWithMaxRetriesAsSkipped(String accountId);

  boolean updateStatusAndIncrementRetryCountForYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatus, List<String> yamlChangeSetIds);

  YamlChangeSet getQueuedChangeSetForWaitingQueueKey(
      String accountId, String queueKey, int maxRunningChangesetsForAccount);

  List<YamlChangeSet> findByAccountIdsStatusLastUpdatedAtLessThan(List<String> runningAccountIdList, long timeout);

  List<String> findDistinctAccountIdsByStatus(Status status);

  UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
      Status oldStatus, Status newStatus, long timeout, String messageCode);

  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);
}
