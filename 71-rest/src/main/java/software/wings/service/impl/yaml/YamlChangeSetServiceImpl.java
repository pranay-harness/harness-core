package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;

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
import io.harness.persistence.ReadPref;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 10/31/17.
 */
@Singleton
@ValidateOnExecution
public class YamlChangeSetServiceImpl implements YamlChangeSetService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PersistentLocker persistentLocker;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EntityUpdateService entityUpdateService;

  private static final Logger logger = LoggerFactory.getLogger(YamlChangeSetServiceImpl.class);

  @Override
  public YamlChangeSet save(YamlChangeSet yamlChangeSet) {
    return wingsPersistence.saveAndGet(YamlChangeSet.class, yamlChangeSet);
  }

  @Override
  public YamlChangeSet get(String accountId, String changeSetId) {
    return wingsPersistence.createQuery(YamlChangeSet.class)
        .filter("accountId", accountId)
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
  public synchronized List<YamlChangeSet> getQueuedChangeSet(String accountId) {
    try (AcquiredLock lock = persistentLocker.acquireLock(YamlChangeSet.class, accountId, Duration.ofMinutes(1))) {
      Query<YamlChangeSet> findQuery = wingsPersistence.createQuery(YamlChangeSet.class)
                                           .filter("accountId", accountId)
                                           .filter("status", Status.QUEUED)
                                           .order("createdAt");
      UpdateOperations<YamlChangeSet> updateOperations =
          wingsPersistence.createUpdateOperations(YamlChangeSet.class).set("status", Status.RUNNING);
      YamlChangeSet modifiedChangeSet =
          wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).findAndModify(findQuery, updateOperations);

      if (modifiedChangeSet == null) {
        logger.info("No change set found in queued state");
        return Arrays.asList();
      }

      return Arrays.asList(modifiedChangeSet);
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }
    return null;
  }

  @Override
  public synchronized List<YamlChangeSet> getChangeSetsToBeMarkedSkipped(String accountId) {
    YamlChangeSet mostRecentCompletedChangeSet = getMostRecentChangeSetWithCompletedStatus(accountId);

    PageRequestBuilder pageRequestBuilder = aPageRequest()
                                                .addFilter("accountId", Operator.EQ, accountId)
                                                .addFilter("status", Operator.IN, new Status[] {Status.QUEUED});

    if (mostRecentCompletedChangeSet != null) {
      pageRequestBuilder.addFilter("createdAt", Operator.GE, mostRecentCompletedChangeSet.getCreatedAt());
    }

    return listYamlChangeSets(pageRequestBuilder.build()).getResponse();
  }

  private YamlChangeSet getMostRecentChangeSetWithCompletedStatus(String accountId) {
    List<YamlChangeSet> changeSetsWithCompletedStatus =
        listYamlChangeSets(aPageRequest()
                               .addFilter("accountId", Operator.EQ, accountId)
                               .addFilter("status", Operator.EQ, Status.COMPLETED)
                               .addOrder("createdAt", OrderType.DESC)
                               .withLimit("1")
                               .build())
            .getResponse();

    return isNotEmpty(changeSetsWithCompletedStatus) ? changeSetsWithCompletedStatus.get(0) : null;
  }

  /**
   * Get all changeSets marked as Failed or Queued.
   * We will combine them together, with recent one overwriting any change for previous one
   * @param accountId
   * @return
   */
  @Override
  public synchronized List<YamlChangeSet> getChangeSetsToSync(String accountId) {
    List<YamlChangeSet> yamlChangeSets = Collections.EMPTY_LIST;
    YamlChangeSet mostRecentCompletedChangeSet = null;
    try (AcquiredLock lock = persistentLocker.acquireLock(YamlChangeSet.class, accountId, Duration.ofMinutes(1))) {
      // Get most recent changeSet with Completed status,
      // We will pick all Queued and Failed changeSets created after this
      mostRecentCompletedChangeSet = getMostRecentChangeSetWithCompletedStatus(accountId);

      PageRequestBuilder pageRequestBuilder = aPageRequest()
                                                  .addFilter("accountId", Operator.EQ, accountId)
                                                  .addFilter("status", Operator.IN, new Status[] {Status.QUEUED})
                                                  .addOrder("createdAt", OrderType.ASC)
                                                  .withLimit("50");

      if (mostRecentCompletedChangeSet != null) {
        pageRequestBuilder.addFilter("createdAt", Operator.GE, mostRecentCompletedChangeSet.getCreatedAt());
      }
      yamlChangeSets = listYamlChangeSets(pageRequestBuilder.build()).getResponse();
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }

    if (isEmpty(yamlChangeSets)) {
      logger.info("No Change set was found for processing for account: " + accountId);
      return Collections.EMPTY_LIST;
    }

    // Update status for these yamlChangeSets to "Running"
    updateStatusForYamlChangeSets(Status.RUNNING,
        wingsPersistence.createQuery(YamlChangeSet.class)
            .field("_id")
            .in(yamlChangeSets.stream().map(yamlChangeSet -> yamlChangeSet.getUuid()).collect(Collectors.toList())));

    return yamlChangeSets;
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
                                                  .filter("accountId", accountId)
                                                  .filter("status", currentStatus);

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
                                                    .filter("accountId", accountId)
                                                    .field("status")
                                                    .in(currentStatuses)
                                                    .field("_id")
                                                    .in(yamlChangeSetIds);

      UpdateResults status = wingsPersistence.update(yamlChangeSetQuery, ops);

      return status.getUpdatedCount() != 0;
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }

    return false;
  }

  @Override
  public boolean deleteChangeSet(String accountId, String changeSetId) {
    return wingsPersistence.delete(wingsPersistence.createQuery(YamlChangeSet.class)
                                       .filter("accountId", accountId)
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
                                                    .addFilter("createdAt", Operator.LT, cutOffTime)
                                                    .addFieldsIncluded("_id")
                                                    .withLimit(batchSize);

        List<YamlChangeSet> yamlChangeSets = listYamlChangeSets(pageRequestBuilder.build()).getResponse();
        if (isNotEmpty(yamlChangeSets)) {
          List<String> ids =
              yamlChangeSets.stream().map(yamlChangeSet -> yamlChangeSet.getUuid()).collect(Collectors.toList());
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
  public <T> YamlChangeSet saveChangeSet(YamlGitConfig yamlGitConfig, List<GitFileChange> gitFileChanges, T entity) {
    if (isEmpty(gitFileChanges)) {
      return null;
    }

    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .accountId(yamlGitConfig.getAccountId())
                                      .gitFileChanges(gitFileChanges)
                                      .status(Status.QUEUED)
                                      .queuedOn(System.currentTimeMillis())
                                      .build();

    yamlChangeSet.setAppId(entityUpdateService.obtainAppIdFromEntity(entity));
    return save(yamlChangeSet);
  }

  private boolean updateStatusForYamlChangeSets(Status desiredStatus, Query<YamlChangeSet> query) {
    UpdateOperations<YamlChangeSet> ops = wingsPersistence.createUpdateOperations(YamlChangeSet.class);
    setUnset(ops, "status", desiredStatus);

    UpdateResults status = wingsPersistence.update(query, ops);
    return status.getUpdatedCount() != 0;
  }
}
