package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HPersistence.returnNewOptions;
import static java.lang.Boolean.TRUE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.Base;
import software.wings.beans.FeatureName;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlChangeSet.YamlChangeSetKeys;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/31/17.
 */
@Singleton
@ValidateOnExecution
@Slf4j
public class YamlChangeSetServiceImpl implements YamlChangeSetService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EntityUpdateService entityUpdateService;

  @Override
  public YamlChangeSet save(YamlChangeSet yamlChangeSet) {
    return wingsPersistence.saveAndGet(YamlChangeSet.class, yamlChangeSet);
  }

  @Override
  public YamlChangeSet get(String accountId, String changeSetId) {
    return wingsPersistence.createQuery(YamlChangeSet.class)
        .filter(YamlChangeSetKeys.accountId, accountId)
        .filter(Mapper.ID_KEY, changeSetId)
        .get();
  }

  @Override
  public void update(YamlChangeSet yamlChangeSet) {
    UpdateOperations<YamlChangeSet> updateOperations =
        wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", yamlChangeSet.getStatus());
    wingsPersistence.update(yamlChangeSet, updateOperations);
  }

  private PageResponse<YamlChangeSet> listYamlChangeSets(PageRequest<YamlChangeSet> pageRequest) {
    return wingsPersistence.query(YamlChangeSet.class, pageRequest);
  }

  @Override
  public synchronized YamlChangeSet getQueuedChangeSetForWaitingAccount(String accountId) {
    try (AcquiredLock lock = persistentLocker.acquireLock(YamlChangeSet.class, accountId, Duration.ofMinutes(2))) {
      if (anyChangeSetRunningForAccount(accountId)) {
        logger.info("Found running changeset for account. Returning null");
        return null;
      }

      final YamlChangeSet selectedChangeSet = selectQueuedChangeSetWithPriority(accountId);

      if (selectedChangeSet == null) {
        logger.info("No change set found in queued state");
      }

      return selectedChangeSet;
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }
    return null;
  }

  private YamlChangeSet selectHeadOfQueue(String accountId) {
    Query<YamlChangeSet> findQuery = wingsPersistence.createQuery(YamlChangeSet.class)
                                         .filter(YamlChangeSetKeys.accountId, accountId)
                                         .filter(YamlChangeSetKeys.status, Status.QUEUED)
                                         .order(YamlChangeSet.CREATED_AT_KEY);

    UpdateOperations<YamlChangeSet> updateOperations =
        wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", Status.RUNNING);
    return wingsPersistence.findAndModify(findQuery, updateOperations, returnNewOptions);
  }

  private YamlChangeSet selectQueuedChangeSetWithPriority(String accountId) {
    //      find the head of the queue
    YamlChangeSet selectedYamlChangeSet = null;

    final YamlChangeSet headChangeSet = peekQueueHead(accountId);
    if (headChangeSet != null && isFullSync(headChangeSet)) {
      selectedYamlChangeSet = headChangeSet;
    }

    if (selectedYamlChangeSet == null) {
      final YamlChangeSet oldestGitToHarnessChangeSet = getOldestGitToHarnessChangeSet(accountId);
      if (oldestGitToHarnessChangeSet != null) {
        selectedYamlChangeSet = oldestGitToHarnessChangeSet;
      }
    }

    if (selectedYamlChangeSet == null) {
      selectedYamlChangeSet = headChangeSet;
    }

    if (selectedYamlChangeSet != null) {
      final boolean updateStatus = updateStatus(accountId, selectedYamlChangeSet.getUuid(), Status.RUNNING);
      if (updateStatus) {
        return get(accountId, selectedYamlChangeSet.getUuid());
      } else {
        logger.error("error while updating status of yaml change set Id = [{}]. Skipping selection",
            selectedYamlChangeSet.getUuid());
      }
    }
    return null;
  }

  private boolean isFullSync(YamlChangeSet yamlChangeSet) {
    return yamlChangeSet.isFullSync();
  }

  private YamlChangeSet peekQueueHead(String accountId) {
    return wingsPersistence.createQuery(YamlChangeSet.class)
        .filter(YamlChangeSetKeys.accountId, accountId)
        .filter(YamlChangeSetKeys.status, Status.QUEUED)
        .project(YamlChangeSetKeys.gitFileChanges, false)
        .order(YamlChangeSet.CREATED_AT_KEY)
        .get();
  }

  private YamlChangeSet getOldestGitToHarnessChangeSet(String accountId) {
    return wingsPersistence.createQuery(YamlChangeSet.class)
        .filter(YamlChangeSetKeys.accountId, accountId)
        .filter(YamlChangeSetKeys.status, Status.QUEUED)
        .filter(YamlChangeSetKeys.gitToHarness, TRUE)
        .project(YamlChangeSetKeys.gitFileChanges, false)
        .order(YamlChangeSet.CREATED_AT_KEY)
        .get();
  }

  private boolean anyChangeSetRunningForAccount(String accountId) {
    return wingsPersistence.createQuery(YamlChangeSet.class)
               .filter(YamlChangeSetKeys.accountId, accountId)
               .filter(YamlChangeSetKeys.status, Status.RUNNING)
               .count()
        > 0;
  }

  @Override
  public synchronized List<YamlChangeSet> getChangeSetsToBeMarkedSkipped(String accountId) {
    YamlChangeSet mostRecentCompletedChangeSet = getMostRecentChangeSetWithCompletedStatus(accountId);

    PageRequestBuilder pageRequestBuilder = aPageRequest()
                                                .addFilter("accountId", Operator.EQ, accountId)
                                                .addFilter("status", Operator.IN, new Status[] {Status.QUEUED});

    if (mostRecentCompletedChangeSet != null) {
      pageRequestBuilder.addFilter(
          YamlChangeSet.CREATED_AT_KEY, Operator.GE, mostRecentCompletedChangeSet.getCreatedAt());
    }

    return listYamlChangeSets(pageRequestBuilder.build()).getResponse();
  }

  private YamlChangeSet getMostRecentChangeSetWithCompletedStatus(String accountId) {
    List<YamlChangeSet> changeSetsWithCompletedStatus =
        listYamlChangeSets(aPageRequest()
                               .addFilter("accountId", Operator.EQ, accountId)
                               .addFilter("status", Operator.EQ, Status.COMPLETED)
                               .addOrder(YamlChangeSet.CREATED_AT_KEY, OrderType.DESC)
                               .withLimit("1")
                               .build())
            .getResponse();

    return isNotEmpty(changeSetsWithCompletedStatus) ? changeSetsWithCompletedStatus.get(0) : null;
  }

  @Override
  public boolean updateStatus(String accountId, String changeSetId, Status newStatus) {
    // replace with acc level batchGit flag
    if (featureFlagService.isEnabled(FeatureName.GIT_BATCH_SYNC, accountId)) {
      return updateStatusForYamlChangeSets(accountId, newStatus, Status.RUNNING);
    }

    YamlChangeSet yamlChangeSet = get(accountId, changeSetId);
    if (yamlChangeSet != null) {
      UpdateResults status = wingsPersistence.update(
          yamlChangeSet, wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", newStatus));
      return status.getUpdatedCount() != 0;
    } else {
      logger.warn("No YamlChangeSet found");
    }
    return false;
  }

  /**
   * Update status from RUNNING to COMPLETED or FAILED depending on operation result
   * @param accountId
   * @param newStatus
   * @return
   */
  @Override
  public boolean updateStatusForYamlChangeSets(String accountId, Status newStatus, Status currentStatus) {
    UpdateOperations<YamlChangeSet> ops = wingsPersistence.createUpdateOperations(YamlChangeSet.class);
    setUnset(ops, "status", newStatus);

    Query<YamlChangeSet> yamlChangeSetQuery = wingsPersistence.createQuery(YamlChangeSet.class)
                                                  .filter(YamlChangeSetKeys.accountId, accountId)
                                                  .filter(YamlChangeSetKeys.status, currentStatus);

    UpdateResults status = wingsPersistence.update(yamlChangeSetQuery, ops);

    return status.getUpdatedCount() != 0;
  }

  @Override
  public boolean updateStatusForGivenYamlChangeSets(
      String accountId, Status newStatus, List<Status> currentStatuses, List<String> yamlChangeSetIds) {
    try (AcquiredLock lock = persistentLocker.acquireLock(YamlChangeSet.class, accountId, Duration.ofMinutes(1))) {
      if (isEmpty(yamlChangeSetIds)) {
        return true;
      }

      UpdateOperations<YamlChangeSet> ops = wingsPersistence.createUpdateOperations(YamlChangeSet.class);
      setUnset(ops, "status", newStatus);

      Query<YamlChangeSet> yamlChangeSetQuery = wingsPersistence.createQuery(YamlChangeSet.class)
                                                    .filter(YamlChangeSetKeys.accountId, accountId)
                                                    .field("status")
                                                    .in(currentStatuses)
                                                    .field("_id")
                                                    .in(yamlChangeSetIds);

      UpdateResults status = wingsPersistence.update(yamlChangeSetQuery, ops);

      return status.getUpdatedCount() != 0;
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }

    return false;
  }

  @Override
  public boolean deleteChangeSet(String accountId, String changeSetId) {
    return wingsPersistence.delete(wingsPersistence.createQuery(YamlChangeSet.class)
                                       .filter(YamlChangeSetKeys.accountId, accountId)
                                       .filter(Mapper.ID_KEY, changeSetId));
  }

  @Override
  public void deleteChangeSets(
      String accountId, Status[] statuses, Integer maxDeleteCount, String batchSize, int retentionPeriodInDays) {
    long cutOffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionPeriodInDays);
    int deletedCount = 0;

    try {
      boolean shouldContinue = true;
      while (shouldContinue && deletedCount < maxDeleteCount) {
        PageRequestBuilder pageRequestBuilder = aPageRequest()
                                                    .addFilter("accountId", Operator.EQ, accountId)
                                                    .addFilter("status", Operator.IN, statuses)
                                                    .addFilter(YamlChangeSet.CREATED_AT_KEY, Operator.LT, cutOffTime)
                                                    .addFieldsIncluded("_id")
                                                    .withLimit(batchSize);

        List<YamlChangeSet> yamlChangeSets = listYamlChangeSets(pageRequestBuilder.build()).getResponse();
        if (isNotEmpty(yamlChangeSets)) {
          List<String> ids = yamlChangeSets.stream().map(Base::getUuid).collect(Collectors.toList());
          wingsPersistence.delete(wingsPersistence.createQuery(YamlChangeSet.class).field("_id").in(ids));
          deletedCount = deletedCount + Integer.parseInt(batchSize);
        } else {
          shouldContinue = false;
        }
      }
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, WingsException.USER)
          .addParam("message", "deleting YamlChangeSets failed with error: " + e.getMessage());
    }
  }

  @Override
  public <T> YamlChangeSet saveChangeSet(String accountId, List<GitFileChange> gitFileChanges, T entity) {
    if (isEmpty(gitFileChanges)) {
      return null;
    }

    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .accountId(accountId)
                                      .gitFileChanges(gitFileChanges)
                                      .status(Status.QUEUED)
                                      .queuedOn(System.currentTimeMillis())
                                      .build();

    yamlChangeSet.setAppId(entityUpdateService.obtainAppIdFromEntity(entity));
    return save(yamlChangeSet);
  }
}
