package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.WingsException;
import io.harness.persistence.GoogleDataStoreAware;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.LogDataStoreService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleLogDataStoreServiceImpl implements LogDataStoreService {
  private static int PURGE_BATCH_SIZE = 10000;
  private static final Logger logger = LoggerFactory.getLogger(GoogleLogDataStoreServiceImpl.class);
  private static final String GOOGLE_APPLICATION_CREDENTIALS_PATH = "GOOGLE_APPLICATION_CREDENTIALS";
  private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private AppService appService;
  private LogDataStoreService mongoLogDataStoreService;

  @Inject
  public GoogleLogDataStoreServiceImpl(AppService appService, WingsPersistence wingsPersistence) {
    String googleCrdentialsPath = System.getenv(GOOGLE_APPLICATION_CREDENTIALS_PATH);
    if (isEmpty(googleCrdentialsPath) || !new File(googleCrdentialsPath).exists()) {
      throw new WingsException("Invalid credentials found at " + googleCrdentialsPath);
    }
    this.appService = appService;
    mongoLogDataStoreService = new MongoLogDataStoreServiceImpl(wingsPersistence);
  }

  @Override
  public <T extends GoogleDataStoreAware> void saveLogs(Class<T> clazz, List<T> logs) {
    if (isEmpty(logs)) {
      return;
    }
    List<Entity> logList = new ArrayList<>();
    logs.forEach(log -> logList.add(log.convertToCloudStorageEntity(datastore)));
    datastore.put(logList.stream().toArray(Entity[] ::new));
  }

  @Override
  public <T extends GoogleDataStoreAware> PageResponse<T> listLogs(Class<T> clazz, PageRequest<T> pageRequest) {
    QueryResults<Entity> results = readResults(clazz, pageRequest);
    int total = getNumberOfResults(clazz, pageRequest);
    List<T> rv = new ArrayList<>();
    while (results.hasNext()) {
      try {
        rv.add((T) clazz.newInstance().readFromCloudStorageEntity(results.next()));
      } catch (Exception e) {
        throw new WingsException(e);
      }
    }

    if (isEmpty(rv)) {
      return mongoLogDataStoreService.listLogs(clazz, pageRequest);
    }

    return aPageResponse()
        .withResponse(rv)
        .withTotal(total)
        .withLimit(pageRequest.getLimit())
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

  private <T extends GoogleDataStoreAware> QueryResults<Entity> readResults(
      Class<T> clazz, PageRequest<T> pageRequest) {
    final Builder queryBuilder = Query.newEntityQueryBuilder()
                                     .setKind(clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                                     .setFilter(createCompositeFilter(pageRequest));
    if (isNotEmpty(pageRequest.getOrders())) {
      pageRequest.getOrders().forEach(sortOrder -> {
        switch (sortOrder.getOrderType()) {
          case DESC:
            queryBuilder.setOrderBy(OrderBy.desc(sortOrder.getFieldName()));
            break;
          case ASC:
            queryBuilder.setOrderBy(OrderBy.asc(sortOrder.getFieldName()));
            break;
          default:
            throw new IllegalStateException("Invalid order type: " + sortOrder.getOrderType());
        }
      });
    }

    if (isNotEmpty(pageRequest.getLimit())) {
      queryBuilder.setLimit(Integer.parseInt(pageRequest.getLimit()));
    }

    if (isNotEmpty(pageRequest.getOffset())) {
      queryBuilder.setOffset(Integer.parseInt(pageRequest.getOffset()));
    }

    return datastore.run(queryBuilder.build());
  }

  private <T extends GoogleDataStoreAware> int getNumberOfResults(Class<T> clazz, PageRequest<T> pageRequest) {
    Query<Key> query = Query.newKeyQueryBuilder()
                           .setKind(clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                           .setFilter(createCompositeFilter(pageRequest))
                           .setLimit(10000)
                           .build();
    List<Key> keys = new ArrayList<>();
    datastore.run(query).forEachRemaining(key -> keys.add(key));
    return keys.size();
  }

  @Nullable
  private <T extends GoogleDataStoreAware> CompositeFilter createCompositeFilter(PageRequest<T> pageRequest) {
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
    return compositeFilter;
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
        throw new IllegalArgumentException("Not supported filter: " + searchFilter);
    }
  }

  public static String readString(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getString(fieldName) : null;
  }

  public static long readLong(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getLong(fieldName) : 0;
  }

  public static byte[] readBlob(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getBlob(fieldName).toByteArray() : null;
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