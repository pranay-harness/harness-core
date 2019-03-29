package software.wings.service.impl;

import static io.harness.beans.PageRequest.UNLIMITED;
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
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.WingsException;
import io.harness.persistence.GoogleDataStoreAware;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DataStoreService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class GoogleDataStoreServiceImpl implements DataStoreService {
  private static int DATA_STORE_BATCH_SIZE = 500;
  private static final Logger logger = LoggerFactory.getLogger(GoogleDataStoreServiceImpl.class);
  private static final String GOOGLE_APPLICATION_CREDENTIALS_PATH = "GOOGLE_APPLICATION_CREDENTIALS";
  private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
  private DataStoreService mongoDataStoreService;

  @Inject
  public GoogleDataStoreServiceImpl(WingsPersistence wingsPersistence) {
    String googleCrdentialsPath = System.getenv(GOOGLE_APPLICATION_CREDENTIALS_PATH);
    if (isEmpty(googleCrdentialsPath) || !new File(googleCrdentialsPath).exists()) {
      throw new WingsException("Invalid credentials found at " + googleCrdentialsPath);
    }
    mongoDataStoreService = new MongoDataStoreServiceImpl(wingsPersistence);
  }

  @Override
  public <T extends GoogleDataStoreAware> void save(Class<T> clazz, List<T> records, boolean ignoreDuplicate) {
    if (isEmpty(records)) {
      return;
    }
    List<List<T>> batches = Lists.partition(records, DATA_STORE_BATCH_SIZE);
    batches.forEach(batch -> {
      List<Entity> logList = new ArrayList<>();
      batch.forEach(record -> logList.add(record.convertToCloudStorageEntity(datastore)));
      datastore.put(logList.stream().toArray(Entity[] ::new));
    });
  }

  @Override
  public <T extends GoogleDataStoreAware> PageResponse<T> list(Class<T> clazz, PageRequest<T> pageRequest) {
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
      return mongoDataStoreService.list(clazz, pageRequest);
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
  public void delete(Class<? extends GoogleDataStoreAware> clazz, String id) {
    Query<Key> query = Query.newKeyQueryBuilder()
                           .setKind(clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                           .setFilter(PropertyFilter.eq("name", id))
                           .build();
    List<Key> keysToDelete = new ArrayList<>();
    datastore.run(query).forEachRemaining(key -> keysToDelete.add(key));
    datastore.delete(keysToDelete.stream().toArray(Key[] ::new));
  }

  @Override
  public void purgeByActivity(String appId, String activityId) {
    mongoDataStoreService.purgeByActivity(appId, activityId);
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
  public void purgeOlderRecords() {
    Reflections reflections = new Reflections("software.wings");
    Set<Class<? extends GoogleDataStoreAware>> dataStoreClasses = reflections.getSubTypesOf(GoogleDataStoreAware.class);
    reflections = new Reflections("io.harness");
    dataStoreClasses.addAll(reflections.getSubTypesOf(GoogleDataStoreAware.class));

    dataStoreClasses.forEach(dataStoreClass -> {
      String collectionName = dataStoreClass.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value();
      logger.info("cleaning up {}", collectionName);
      Query<Key> query = Query.newKeyQueryBuilder()
                             .setKind(collectionName)
                             .setFilter(PropertyFilter.lt("validUntil", System.currentTimeMillis()))
                             .build();
      List<Key> keysToDelete = new ArrayList<>();
      datastore.run(query).forEachRemaining(key -> keysToDelete.add(key));
      logger.info("Total keys to delete {} for {}", keysToDelete.size(), collectionName);
      final List<List<Key>> keyBatches = Lists.partition(keysToDelete, DATA_STORE_BATCH_SIZE);
      keyBatches.forEach(keys -> {
        logger.info("purging {} records from {}", keys.size(), collectionName);
        datastore.delete(keys.stream().toArray(Key[] ::new));
      });
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

    if (isNotEmpty(pageRequest.getLimit()) && !pageRequest.getLimit().equals(UNLIMITED)) {
      queryBuilder.setLimit(Integer.parseInt(pageRequest.getLimit()));
    }

    if (isNotEmpty(pageRequest.getOffset())) {
      queryBuilder.setOffset(Integer.parseInt(pageRequest.getOffset()));
    }

    return datastore.run(queryBuilder.build());
  }

  private <T extends GoogleDataStoreAware> int getNumberOfResults(Class<T> clazz, PageRequest<T> pageRequest) {
    if (isEmpty(pageRequest.getLimit()) || pageRequest.getLimit().equals(UNLIMITED)) {
      return 0;
    }

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
      pageRequest.getFilters()
          .stream()
          .filter(searchFilter -> searchFilter.getFieldValues()[0] != null)
          .forEach(searchFilter -> propertyFilters.add(createFilter(searchFilter)));
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
        if (searchFilter.getFieldValues()[0] instanceof String) {
          return PropertyFilter.eq(searchFilter.getFieldName(), (String) searchFilter.getFieldValues()[0]);
        } else if (searchFilter.getFieldValues()[0].getClass() != null
            && searchFilter.getFieldValues()[0].getClass().isEnum()) {
          return PropertyFilter.eq(searchFilter.getFieldName(), ((Enum) searchFilter.getFieldValues()[0]).name());
        } else {
          return PropertyFilter.eq(searchFilter.getFieldName(), (long) searchFilter.getFieldValues()[0]);
        }

      case LT:
        return PropertyFilter.lt(
            searchFilter.getFieldName(), Long.parseLong(String.valueOf(searchFilter.getFieldValues()[0])));

      case LT_EQ:
        return PropertyFilter.le(
            searchFilter.getFieldName(), Long.parseLong(String.valueOf(searchFilter.getFieldValues()[0])));

      case GT:
        return PropertyFilter.gt(
            searchFilter.getFieldName(), Long.parseLong(String.valueOf(searchFilter.getFieldValues()[0])));

      case GE:
        return PropertyFilter.ge(
            searchFilter.getFieldName(), Long.parseLong(String.valueOf(searchFilter.getFieldValues()[0])));

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

  public static void addFieldIfNotEmpty(
      com.google.cloud.datastore.Entity.Builder builder, String key, String value, boolean excludeFromIndex) {
    if (isEmpty(value)) {
      return;
    }

    builder.set(key, StringValue.newBuilder(value).setExcludeFromIndexes(excludeFromIndex).build());
  }
}