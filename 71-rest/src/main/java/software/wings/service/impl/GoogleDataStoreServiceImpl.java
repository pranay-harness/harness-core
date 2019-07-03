package software.wings.service.impl;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.WingsException;
import io.harness.persistence.GoogleDataStoreAware;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import software.wings.beans.Log;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DataStoreService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
@Slf4j
public class GoogleDataStoreServiceImpl implements DataStoreService {
  private static int DATA_STORE_BATCH_SIZE = 500;

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

    return aPageResponse()
        .withResponse(rv)
        .withTotal(total)
        .withLimit(pageRequest.getLimit())
        .withOffset(pageRequest.getOffset())
        .withFilters(pageRequest.getFilters())
        .withOrders(pageRequest.getOrders())
        .build();
  }

  public <T extends GoogleDataStoreAware> T getEntity(Class<T> clazz, String id) {
    Key keyToFetch = datastore.newKeyFactory()
                         .setKind(clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                         .newKey(id);
    try {
      return (T) clazz.newInstance().readFromCloudStorageEntity(datastore.get(keyToFetch));
    } catch (Exception ex) {
      throw new WingsException(ex);
    }
  }

  public <T extends GoogleDataStoreAware> void incrementField(
      Class<T> clazz, String id, String fieldName, int incrementCount) {
    Transaction txn = datastore.newTransaction();
    Key keyToFetch = datastore.newKeyFactory()
                         .setKind(clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                         .newKey(id);
    Entity entity = txn.get(keyToFetch);
    Entity updatedEntity = Entity.newBuilder(entity).set(fieldName, entity.getLong(fieldName) + incrementCount).build();
    txn.put(updatedEntity);
    txn.commit();
  }

  @Override
  public void delete(Class<? extends GoogleDataStoreAware> clazz, String id) {
    logger.info("Deleting from GoogleDatastore table {}, id: {}",
        clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value(), id);
    Key keyToDelete = datastore.newKeyFactory()
                          .setKind(clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value())
                          .newKey(id);
    datastore.delete(keyToDelete);
    logger.info("Deleted from GoogleDatastore table {}, id: {}",
        clazz.getAnnotation(org.mongodb.morphia.annotations.Entity.class).value(), id);
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
    String field = searchFilter.getFieldName().equals("uuid") ? "__key__" : searchFilter.getFieldName();
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

  public static <T> List readList(Entity entity, String fieldName, Class<T> clazz) {
    List list = entity.contains(fieldName) ? entity.getList(fieldName) : null;
    if (list == null) {
      return null;
    }
    Class gdsType = StringValue.class;
    if (clazz.equals(String.class)) {
      gdsType = StringValue.class;
    } else if (clazz.equals(Double.class)) {
      gdsType = DoubleValue.class;
    } else if (clazz.equals(Long.class)) {
      gdsType = LongValue.class;
    } else if (clazz.equals(Boolean.class)) {
      gdsType = BooleanValue.class;
    }

    List output = new ArrayList<>();
    for (Object entry : list) {
      if (StringValue.class.isInstance(entry) && clazz.equals(String.class)) {
        output.add(((StringValue) entry).get());
      } else if (DoubleValue.class.isInstance(entry) && clazz.equals(Double.class)) {
        output.add(((DoubleValue) entry).get());
      } else if (LongValue.class.isInstance(entry) && clazz.equals(Long.class)) {
        output.add(((LongValue) entry).get());
      } else if (BooleanValue.class.isInstance(entry) && clazz.equals(Boolean.class)) {
        output.add(((BooleanValue) entry).get());
      }
    }
    return output;
  }

  public static long readLong(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getLong(fieldName) : 0;
  }

  public static double readDouble(Entity entity, String fieldName) {
    return entity.contains(fieldName) ? entity.getDouble(fieldName) : 0.0;
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

  public static void addFieldIfNotEmpty(
      com.google.cloud.datastore.Entity.Builder builder, String key, long value, boolean excludeFromIndex) {
    builder.set(key, LongValue.newBuilder(value).setExcludeFromIndexes(excludeFromIndex).build());
  }

  public static <T> void addFieldIfNotEmpty(com.google.cloud.datastore.Entity.Builder builder, String key, Set value,
      boolean excludeFromIndex, Class<T> clazz) {
    if (isEmpty(value)) {
      logger.info("Value is empty, nothing to store");
      return;
    }

    com.google.cloud.datastore.ListValue.Builder listBuilder = ListValue.newBuilder();

    for (Object entry : value) {
      if (clazz.equals(Integer.class)) {
        listBuilder.addValue(((Integer) entry).longValue());
      } else if (clazz.equals(Long.class)) {
        listBuilder.addValue((Long) entry);
      } else if (clazz.equals(Double.class)) {
        listBuilder.addValue((Double) entry);
      } else if (clazz.equals(String.class)) {
        listBuilder.addValue((String) entry);
      } else {
        logger.error("GDS addField in list: Class not supported {}", entry.getClass());
      }
    }

    ListValue listValue = listBuilder.build();
    builder.set(key, listValue);
  }

  public static void addFieldIfNotEmpty(
      com.google.cloud.datastore.Entity.Builder builder, String key, Blob value, boolean excludeFromIndex) {
    if (value == null) {
      return;
    }

    builder.set(key, BlobValue.newBuilder(value).setExcludeFromIndexes(excludeFromIndex).build());
  }
}