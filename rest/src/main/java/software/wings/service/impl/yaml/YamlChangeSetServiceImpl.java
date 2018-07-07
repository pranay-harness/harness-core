package software.wings.service.impl.yaml;

import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.FeatureName;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.Status;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
      YamlChangeSet modifiedChangeSet = wingsPersistence.getDatastore().findAndModify(findQuery, updateOperations);

      if (modifiedChangeSet == null) {
        logger.info("No change set found in queued state");
        return Arrays.asList();
      }

      return Arrays.asList(modifiedChangeSet);
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }
    return null;
  }

  @Override
  public synchronized List<YamlChangeSet> getChangeSetsToBeMarkedSkipped(String accountId) {
    YamlChangeSet mostRecentCompletedChangeSet = getMostRecentChangeSetWithCompletedStatus(accountId);

    PageRequestBuilder pageRequestBuilder =
        aPageRequest()
            .addFilter("accountId", Operator.EQ, accountId)
            .addFilter("status", Operator.IN, new Status[] {Status.QUEUED, Status.FAILED});

    if (mostRecentCompletedChangeSet != null) {
      pageRequestBuilder.addFilter("createdAt", Operator.GT, mostRecentCompletedChangeSet.getCreatedAt());
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

    return EmptyPredicate.isNotEmpty(changeSetsWithCompletedStatus) ? changeSetsWithCompletedStatus.get(0) : null;
  }

  /**
   * Get all changeSets marked as Failed or Queued.
   * We will combine them together, with recent one overwriting any change for previous one
   * @param accountId
   * @return
   */
  @Override
  public synchronized List<YamlChangeSet> getChangeSetsToSync(String accountId) {
    try (AcquiredLock lock = persistentLocker.acquireLock(YamlChangeSet.class, accountId, Duration.ofMinutes(1))) {
      // Get most recent changeSet with Completed status,
      // We will pick all Queued and Failed changeSets created after this
      YamlChangeSet mostRecentCompletedChangeSet = getMostRecentChangeSetWithCompletedStatus(accountId);

      PageRequestBuilder pageRequestBuilder =
          aPageRequest()
              .addFilter("accountId", Operator.EQ, accountId)
              .addFilter("status", Operator.IN, new Status[] {Status.QUEUED, Status.FAILED})
              .addOrder("createdAt", OrderType.ASC)
              .withLimit("50");

      if (mostRecentCompletedChangeSet != null) {
        pageRequestBuilder.addFilter("createdAt", Operator.GT, mostRecentCompletedChangeSet.getCreatedAt());
      }
      List<YamlChangeSet> yamlChangeSets = listYamlChangeSets(pageRequestBuilder.build()).getResponse();

      if (EmptyPredicate.isEmpty(yamlChangeSets)) {
        logger.info("No Change set was found for processing for account: " + accountId);
      }

      // Update status for these yamlChangeSets to "Running"
      updateStatusForYamlChangeSets(Status.RUNNING,
          wingsPersistence.createQuery(YamlChangeSet.class)
              .field("_id")
              .in(yamlChangeSets.stream().map(yamlChangeSet -> yamlChangeSet.getUuid()).collect(Collectors.toList())));

      return yamlChangeSets;
    } catch (WingsException exception) {
      exception.logProcessedMessages(MANAGER, logger);
    } catch (Exception exception) {
      logger.error("Error seen in fetching changeSet", exception);
    }
    return null;
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
      if (yamlChangeSetIds == null) {
        yamlChangeSetIds = new ArrayList<>();
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
      exception.logProcessedMessages(MANAGER, logger);
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
  public void saveChangeSet(YamlGitConfig yamlGitConfig, List<GitFileChange> gitFileChanges) {
    YamlChangeSet yamlChangeSet = YamlChangeSet.builder()
                                      .accountId(yamlGitConfig.getAccountId())
                                      .gitFileChanges(gitFileChanges)
                                      .status(Status.QUEUED)
                                      .queuedOn(System.currentTimeMillis())
                                      .build();
    yamlChangeSet.setAppId(Base.GLOBAL_APP_ID);
    save(yamlChangeSet);
  }

  private boolean updateStatusForYamlChangeSets(Status desiredStatus, Query<YamlChangeSet> query) {
    UpdateOperations<YamlChangeSet> ops = wingsPersistence.createUpdateOperations(YamlChangeSet.class);
    setUnset(ops, "status", desiredStatus);

    UpdateResults status = wingsPersistence.update(query, ops);
    return status.getUpdatedCount() != 0;
  }
}
