package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.System.currentTimeMillis;
import static software.wings.beans.Log.Builder.aLog;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.FeatureName;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LogDataStoreService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleLogDataLogStoreServiceImpl implements LogDataStoreService {
  private static int PURGE_BATCH_SIZE = 10000;
  private static final Logger logger = LoggerFactory.getLogger(GoogleLogDataLogStoreServiceImpl.class);
  private static final String GOOGLE_APPLICATION_CREDENTIALS_PATH = "GOOGLE_APPLICATION_CREDENTIALS";
  private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private AppService appService;
  private FeatureFlagService featureFlagService;
  private MongoLogDataStoreServiceImpl mongoLogDataStoreService;

  @Inject
  public GoogleLogDataLogStoreServiceImpl(
      AppService appService, FeatureFlagService featureFlagService, WingsPersistence wingsPersistence) {
    String googleCrdentialsPath = System.getenv(GOOGLE_APPLICATION_CREDENTIALS_PATH);
    if (isEmpty(googleCrdentialsPath) || !new File(googleCrdentialsPath).exists()) {
      throw new WingsException("Invalid credentials found at " + googleCrdentialsPath);
    }
    this.appService = appService;
    this.featureFlagService = featureFlagService;
    mongoLogDataStoreService = new MongoLogDataStoreServiceImpl(wingsPersistence);
  }

  @Override
  public void saveExecutionLog(List<Log> logs) {
    if (logs == null) {
      return;
    }
    try {
      List<Entity> logList = new ArrayList<>();
      logs.forEach(log -> logList.add(convertToCloudStorageEntity(datastore, log)));
      datastore.put(logList.stream().toArray(Entity[] ::new));
    } catch (Exception e) {
      logger.error("Error saving execution logs", e);
    }
    mongoLogDataStoreService.saveExecutionLog(logs);
  }

  @Override
  public PageResponse<Log> listExecutionLog(String appId, PageRequest<Log> pageRequest) {
    if (!featureFlagService.isEnabled(FeatureName.GCD_STORAGE, appService.getAccountIdByAppId(appId))) {
      return mongoLogDataStoreService.listExecutionLog(appId, pageRequest);
    }

    CompositeFilter compositeFilter = null;
    if (isNotEmpty(pageRequest.getFilters())) {
      List<PropertyFilter> propertyFilters = new ArrayList<>();
      pageRequest.getFilters().forEach(searchFilter -> propertyFilters.add(createFilter(searchFilter)));
      if (propertyFilters.size() == 1) {
        compositeFilter = CompositeFilter.and(propertyFilters.get(0));
      } else {
        compositeFilter = CompositeFilter.and(propertyFilters.get(0),
            propertyFilters.subList(1, propertyFilters.size()).toArray(new PropertyFilter[propertyFilters.size() - 1]));
      }
    }

    Query<Entity> query = Query.newEntityQueryBuilder()
                              .setKind(Log.class.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                              .setFilter(compositeFilter)
                              .setOrderBy(OrderBy.asc("createdAt"))
                              .build();
    QueryResults<Entity> results = datastore.run(query);
    List<Log> rv = new ArrayList<>();
    while (results.hasNext()) {
      rv.add(readLogFromCloudStore(results.next()));
    }

    if (isEmpty(rv)) {
      rv = mongoLogDataStoreService.listExecutionLog(appId, pageRequest).getResponse();
    }

    return aPageResponse()
        .withResponse(rv)
        .withTotal(rv.size())
        .withOffset(pageRequest.getOffset())
        .withFilters(pageRequest.getFilters())
        .withOrders(pageRequest.getOrders())
        .build();
  }

  @Override
  public void purgeByActivity(String appId, String activityId) {
    mongoLogDataStoreService.purgeByActivity(appId, activityId);
    Query<Key> query = Query.newKeyQueryBuilder()
                           .setKind(Log.class.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                           .setFilter(CompositeFilter.and(
                               PropertyFilter.eq("appId", appId), PropertyFilter.eq("activityId", activityId)))
                           .build();
    List<Key> keysToDelete = new ArrayList<>();
    datastore.run(query).forEachRemaining(key -> keysToDelete.add(key));
    logger.info("deleting {} keys for activity {}", keysToDelete.size(), activityId);
    datastore.delete(keysToDelete.stream().toArray(Key[] ::new));
  }

  @Override
  public void purgeOlderLogs() {
    Query<Key> query =
        Query.newKeyQueryBuilder()
            .setKind(Log.class.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
            .setFilter(PropertyFilter.lt("createdAt", System.currentTimeMillis() - TimeUnit.DAYS.toMillis(180)))
            .build();
    List<Key> keysToDelete = new ArrayList<>();
    datastore.run(query).forEachRemaining(key -> keysToDelete.add(key));
    logger.info("Total keys to delete {}", keysToDelete.size());
    final List<List<Key>> keyBatches = batchKeysToDelete(keysToDelete);
    keyBatches.forEach(keys -> {
      logger.info("purging {} records", keys.size());
      datastore.delete(keys.stream().toArray(Key[] ::new));
    });
  }

  private PropertyFilter createFilter(SearchFilter searchFilter) {
    switch (searchFilter.getOp()) {
      case EQ:
        return searchFilter.getFieldValues()[0] instanceof String
            ? PropertyFilter.eq(searchFilter.getFieldName(), (String) searchFilter.getFieldValues()[0])
            : PropertyFilter.eq(searchFilter.getFieldName(), (Long) searchFilter.getFieldValues()[0]);

      case LT:
        return PropertyFilter.lt(searchFilter.getFieldName(), (Long) searchFilter.getFieldValues()[0]);

      case LT_EQ:
        return PropertyFilter.le(searchFilter.getFieldName(), (Long) searchFilter.getFieldValues()[0]);

      case GT:
        return PropertyFilter.gt(searchFilter.getFieldName(), (Long) searchFilter.getFieldValues()[0]);

      case GE:
        return PropertyFilter.ge(searchFilter.getFieldName(), (Long) searchFilter.getFieldValues()[0]);

      default:
        throw new IllegalArgumentException("Not supported filer: " + searchFilter);
    }
  }

  private com.google.cloud.datastore.Entity convertToCloudStorageEntity(Datastore datastore, Log log) {
    Key taskKey = datastore.newKeyFactory()
                      .setKind(Log.class.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                      .newKey(generateUuid());
    com.google.cloud.datastore.Entity.Builder logEntityBuilder =
        com.google.cloud.datastore.Entity.newBuilder(taskKey)
            .set("activityId", log.getActivityId())
            .set("logLine", StringValue.newBuilder(log.getLogLine()).setExcludeFromIndexes(true).build())
            .set("linesCount", LongValue.newBuilder(log.getLinesCount()).setExcludeFromIndexes(true).build())
            .set("logLevel", StringValue.newBuilder(log.getLogLevel().toString()).setExcludeFromIndexes(true).build())
            .set("commandExecutionStatus",
                StringValue.newBuilder(log.getCommandExecutionStatus().name()).setExcludeFromIndexes(true).build())
            .set("createdAt", currentTimeMillis());
    if (isNotEmpty(log.getHostName())) {
      logEntityBuilder.set("hostName", log.getHostName());
    }

    if (isNotEmpty(log.getAppId())) {
      logEntityBuilder.set("appId", log.getAppId());
    }

    if (isNotEmpty(log.getCommandUnitName())) {
      logEntityBuilder.set("commandUnitName", log.getCommandUnitName());
    }

    return logEntityBuilder.build();
  }

  private Log readLogFromCloudStore(com.google.cloud.datastore.Entity entity) {
    return aLog()
        .withUuid(entity.getKey().getName())
        .withActivityId(readString(entity, "activityId"))
        .withLogLine(readString(entity, "logLine"))
        .withLogLevel(LogLevel.valueOf(readString(entity, "logLevel")))
        .withCreatedAt(readLong(entity, "createdAt"))
        .withHostName(readString(entity, "hostName"))
        .withAppId(readString(entity, "appId"))
        .withCommandUnitName(readString(entity, "commandUnitName"))
        .build();
  }

  private String readString(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getString(fieldName) : null;
  }

  private long readLong(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getLong(fieldName) : 0;
  }

  private List<List<Key>> batchKeysToDelete(List<Key> keys) {
    List<List<Key>> keyBatches = new ArrayList<>();
    List<Key> keyBatch = new ArrayList<>();
    for (Key key : keys) {
      keyBatch.add(key);
      if (keyBatch.size() >= PURGE_BATCH_SIZE) {
        keyBatches.add(keyBatch);
        keyBatch = new ArrayList<>();
      }
    }

    if (!keyBatch.isEmpty()) {
      keyBatches.add(keyBatch);
    }

    return keyBatches;
  }
}
