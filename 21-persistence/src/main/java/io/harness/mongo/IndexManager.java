package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableSet;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import com.mongodb.ReadPreference;
import io.harness.annotation.IgnoreUnusedIndex;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.MorphiaMove.MorphiaMoveKeys;
import io.harness.mongo.SampleEntity.SampleEntityKeys;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@UtilityClass
@Slf4j
public class IndexManager {
  public static final String UNIQUE = "unique";
  public static final String BACKGROUND = "background";
  public static final String SPARSE = "sparse";
  public static final String NAME = "name";
  public static final String EXPIRE_AFTER_SECONDS = "expireAfterSeconds";
  public static final Duration SOAKING_PERIOD = ofDays(1);
  // We do not want to drop temporary index before we created the new one. Make the soaking period smaller.
  public static final Duration REBUILD_SOAKING_PERIOD = SOAKING_PERIOD.minus(ofHours(1));

  @Value
  @Builder
  public static class IndexCreator {
    private DBCollection collection;
    private BasicDBObject keys;
    private BasicDBObject options;

    public void create() {
      logger.warn("Creating index {} {}", options.toString(), keys.toString());
      collection.createIndex(keys, options);
    }
  }

  public static void dropIndex(DBCollection collection, String indexName) {
    logger.warn("Dropping index {}", indexName);
    collection.dropIndex(indexName);
  }

  @Value
  @AllArgsConstructor
  static class Accesses {
    int operations;
    Date since;
  }

  private static Map<String, Accesses> extractAccesses(Cursor cursor) {
    Map<String, Accesses> accessesMap = new HashMap<>();

    while (cursor.hasNext()) {
      BasicDBObject object = (BasicDBObject) cursor.next();

      String name = (String) object.get(NAME);
      BasicDBObject accessesObject = (BasicDBObject) object.get("accesses");

      Accesses accesses =
          new Accesses(((Long) accessesObject.get("ops")).intValue(), (Date) accessesObject.get("since"));

      accessesMap.put(name, accesses);
    }
    return accessesMap;
  }

  private static Map<String, Accesses> mergeAccesses(
      Map<String, Accesses> accessesPrimary, Map<String, Accesses> accessesSecondary) {
    Map<String, Accesses> accessesMap = new HashMap<>();

    for (Entry<String, Accesses> entry : accessesPrimary.entrySet()) {
      String index = entry.getKey();
      Accesses primary = entry.getValue();
      Accesses secondary = accessesSecondary.get(entry.getKey());
      if (secondary == null) {
        accessesMap.put(index, primary);
      } else {
        accessesMap.put(index,
            new Accesses(primary.getOperations() + secondary.getOperations(),
                primary.getSince().before(secondary.getSince()) ? primary.getSince() : secondary.getSince()));
      }
    }

    for (Entry<String, Accesses> entry : accessesSecondary.entrySet()) {
      accessesMap.putIfAbsent(entry.getKey(), entry.getValue());
    }

    return accessesMap;
  }

  public static DBObject findIndexByFields(IndexCreator indexCreator) {
    Set keysSet = indexCreator.getKeys().toMap().keySet();
    List<DBObject> indexInfo = indexCreator.getCollection().getIndexInfo();
    for (DBObject index : indexInfo) {
      DBObject indexKeys = (DBObject) index.get("key");
      Set indexKeysSet = indexKeys.toMap().keySet();
      if (!indexKeysSet.containsAll(keysSet)) {
        continue;
      }
      if (!keysSet.containsAll(indexKeysSet)) {
        continue;
      }

      return index;
    }
    return null;
  }

  public static boolean isRebuildNeeded(IndexCreator indexCreator) {
    // first make sure that we need to rename the index
    DBObject indexByFields = findIndexByFields(indexCreator);
    if (indexByFields == null) {
      return false;
    }

    String name = (String) indexCreator.options.get(NAME);
    String currentName = (String) indexByFields.get(NAME);

    return !currentName.equals(name);
  }

  public static void rebuildIndex(IndexCreator indexCreator, Duration waitFor) {
    DBObject indexByFields = findIndexByFields(indexCreator);
    if (indexByFields == null) {
      return;
    }

    String currentName = (String) indexByFields.get(NAME);

    // Mongo does not support index updates - we have to recreate the index

    // We do not want to have to make queries without an index. We cannot create another one though, because mongo
    // will detect that we already have such with the same set of fields.
    // We are going to add an extra dummy field. this is safe because fields at the end will not prevent the index to
    // be used for other purposes.
    BasicDBObject tempKeys = (BasicDBObject) indexCreator.keys.copy();
    tempKeys.put("very_dummy_field", 1);

    // We also have to pick another name
    BasicDBObject tempOptions = (BasicDBObject) indexCreator.options.copy();
    tempOptions.put(NAME, "TRI_" + currentTimeMillis());

    IndexCreator tempCreator =
        IndexCreator.builder().collection(indexCreator.getCollection()).keys(tempKeys).options(tempOptions).build();

    // Lets see if we already have this one created from before
    DBObject triIndex = findIndexByFields(tempCreator);
    if (triIndex == null) {
      tempCreator.create();
      triIndex = findIndexByFields(tempCreator);
      // that is unlikely but just in case
      if (triIndex == null) {
        return;
      }
    }

    // Check if the temporary index soaked enough
    long indexTime = Long.parseLong(triIndex.get(NAME).toString().substring(4));
    if (indexTime + waitFor.toMillis() > currentTimeMillis()) {
      return;
    }

    // now we are safe to drop the original
    dropIndex(indexCreator.collection, currentName);

    // Lets create the target index
    indexCreator.create();
  }

  // This for checks for unused indexes. It utilize the indexStat provided from mongo.
  // A good article on the topic:
  // https://www.objectrocket.com/blog/mongodb/considerations-for-using-indexstats-to-find-unused-indexes-in-mongodb/
  // NOTE: This is work in progress. For the time being we are checking only for completely unused indexes.
  private static void checkForUnusedIndexes(DBCollection collection, Map<String, Accesses> accesses) {
    long now = currentTimeMillis();
    Date tooNew = new Date(now - ofDays(7).toMillis());

    List<DBObject> indexInfo = collection.getIndexInfo();
    Set<String> uniqueIndexes = indexInfo.stream()
                                    .filter(obj -> {
                                      Object unique = obj.get(UNIQUE);
                                      return unique != null && unique.toString().equals("true");
                                    })
                                    .map(obj -> obj.get(NAME).toString())
                                    .collect(toSet());

    accesses.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getOperations() == 0)
        .filter(entry -> entry.getValue().getSince().compareTo(tooNew) < 0)
        // Exclude the object id index. It is rare but it might be unused
        .filter(entry -> !entry.getKey().equals("_id_"))
        // Exclude ttl indexes, Ttl monitoring is not tracked as operations
        .filter(entry -> !entry.getKey().startsWith("validUntil"))
        // Exclude unique indexes. Adding items is not tracked as index operations
        .filter(entry -> !uniqueIndexes.contains(entry.getKey()))
        // Temporary exclude indexes that coming from Base class. Currently we have no flexibility to enable/disable
        // such for different objects.
        .filter(entry -> !entry.getKey().startsWith("appId"))
        .filter(entry -> !entry.getKey().startsWith(SampleEntityKeys.createdAt))
        // Alert for every index that left:
        .forEach(entry -> {
          Duration passed = Duration.between(entry.getValue().getSince().toInstant(), ZonedDateTime.now().toInstant());
          try (AutoLogContext ignore = new IndexLogContext(entry.getKey(), OVERRIDE_ERROR)) {
            logger.error("Index is not used at for {} days", passed.toDays());
          }
        });
  }

  static Map<String, Accesses> fetchIndexAccesses(DBCollection collection) {
    Map<String, Accesses> accessesPrimary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.primary()));
    Map<String, Accesses> accessesSecondary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.secondary()));

    return mergeAccesses(accessesPrimary, accessesSecondary);
  }

  public static void updateMovedClasses(
      AdvancedDatastore primaryDatastore, Map<String, Class> morphiaInterfaceImplementers) {
    Map<String, String> movements = movements(morphiaInterfaceImplementers);

    movements.forEach((source, target) -> {
      Query<MorphiaMove> query = primaryDatastore.createQuery(MorphiaMove.class).filter(MorphiaMoveKeys.target, target);
      UpdateOperations<MorphiaMove> updateOperations =
          primaryDatastore.createUpdateOperations(MorphiaMove.class).addToSet(MorphiaMoveKeys.sources, source);
      primaryDatastore.findAndModify(query, updateOperations, HPersistence.upsertReturnNewOptions);
    });
  }

  public static Map<String, String> movements(Map<String, Class> morphiaInterfaceImplementers) {
    Map<String, String> movements = new HashMap<>();
    for (Entry<String, Class> entry : morphiaInterfaceImplementers.entrySet()) {
      if (entry.getValue() == MorphiaRegistrar.NotFoundClass.class) {
        continue;
      }
      String target = entry.getValue().getName();
      if (entry.getKey().equals(target)) {
        continue;
      }

      movements.put(entry.getKey(), target);
    }
    return movements;
  }

  interface IndexesProcessor {
    void process(MappedClass mc, DBCollection collection);
  }

  public static Set<String> processIndexes(AdvancedDatastore datastore, Morphia morphia, IndexesProcessor processor) {
    Set<String> processedCollections = new HashSet<>();
    Collection<MappedClass> mappedClasses = morphia.getMapper().getMappedClasses();
    mappedClasses.forEach(mc -> {
      if (mc.getEntityAnnotation() == null) {
        return;
      }

      DBCollection collection = datastore.getCollection(mc.getClazz());
      try (AutoLogContext ignore = new CollectionLogContext(collection.getName(), OVERRIDE_ERROR)) {
        if (processedCollections.contains(collection.getName())) {
          return;
        }
        processedCollections.add(collection.getName());

        processor.process(mc, collection);
      }
    });
    return processedCollections;
  }

  public static List<IndexCreator> allIndexes(AdvancedDatastore datastore, Morphia morphia) {
    List<IndexCreator> result = new ArrayList<>();
    processIndexes(datastore, morphia, (mc, collection) -> result.addAll(indexCreators(mc, collection).values()));
    return result;
  }

  public static void ensureIndexes(AdvancedDatastore datastore, Morphia morphia) {
    // Morphia auto creates embedded/nested Entity indexes with the parent Entity indexes.
    // There is no way to override this behavior.
    // https://github.com/mongodb/morphia/issues/706

    Set<String> processedCollections = processIndexes(datastore, morphia, (mc, collection) -> {
      try {
        Map<String, IndexCreator> creators = indexCreators(mc, collection);
        // We should be attempting to drop indexes only if we successfully created all new ones
        boolean okToDropIndexes = createNewIndexes(creators) == creators.size();

        Map<String, Accesses> accesses = fetchIndexAccesses(collection);

        if (okToDropIndexes) {
          List<DBObject> indexInfo = collection.getIndexInfo();
          List<String> obsoleteIndexes = indexInfo.stream()
                                             .map(obj -> obj.get(NAME).toString())
                                             .filter(name -> !"_id_".equals(name))
                                             .filter(name -> !creators.keySet().contains(name))
                                             .collect(toList());

          if (isNotEmpty(obsoleteIndexes)) {
            // Make sure that all indexes that we have are operational, we check that they have being seen since
            // at least a day
            Date tooNew = tooNew();
            okToDropIndexes = isOkToDropIndexes(tooNew, accesses, indexInfo);

            if (okToDropIndexes) {
              obsoleteIndexes.forEach(name -> {
                try (AutoLogContext ignore2 = new IndexLogContext(name, OVERRIDE_ERROR)) {
                  dropIndex(collection, name);
                } catch (RuntimeException ex) {
                  logger.error("Failed to drop index", ex);
                }
              });
            }
          }
        }

        if (mc.getClazz().getAnnotation(IgnoreUnusedIndex.class) == null) {
          checkForUnusedIndexes(collection, accesses);
        }
      } catch (RuntimeException exception) {
        logger.error("", exception);
      }
    });

    Set<String> whitelistCollections = ImmutableSet.<String>of(
        // Files and chunks
        "artifacts.chunks", "artifacts.files", "audits.chunks", "audits.files", "configs.chunks", "configs.files",
        "platforms.chunks", "platforms.files", "profile_results.chunks", "profile_results.files",
        "terraform_state.chunks", "terraform_state.files",
        // Quartz
        "quartz_calendars", "quartz_jobs", "quartz_locks", "quartz_schedulers", "quartz_triggers",
        // Quartz Verification
        "quartz_verification_calendars", "quartz_verification_jobs", "quartz_verification_locks",
        "quartz_verification_schedulers", "quartz_verification_triggers",
        // Persistent locks
        "locks",
        // verification service
        "timeSeriesAnomaliesRecords", "timeSeriesCumulativeSums");

    List<String> obsoleteCollections = datastore.getDB()
                                           .getCollectionNames()
                                           .stream()
                                           .filter(name -> !processedCollections.contains(name))
                                           .filter(name -> !whitelistCollections.contains(name))
                                           .filter(name -> !name.startsWith("!!!test"))
                                           .collect(toList());

    if (isNotEmpty(obsoleteCollections)) {
      logger.error("Unknown mongo collections detected: {}\n"
              + "Please create migration to delete them or add them to the whitelist.",
          join(", ", obsoleteCollections));
    }
  }

  static Date tooNew() {
    return new Date(currentTimeMillis() - SOAKING_PERIOD.toMillis());
  }

  static boolean isOkToDropIndexes(Date tooNew, Map<String, Accesses> accesses, List<DBObject> indexInfo) {
    for (DBObject info : indexInfo) {
      String name = info.get(NAME).toString();
      // Note that we are aware that we checking the obsolete indexes as well. This is to play on the safe side.
      Accesses access = accesses.get(name);
      if (access == null || access.getSince().compareTo(tooNew) > 0) {
        return false;
      }
    }
    return true;
  }

  static int createNewIndexes(Map<String, IndexCreator> creators) {
    int created = 0;
    for (Entry<String, IndexCreator> creator : creators.entrySet()) {
      String name = creator.getKey();
      try (AutoLogContext ignore = new IndexLogContext(name, OVERRIDE_ERROR)) {
        IndexCreator indexCreator = creator.getValue();
        try {
          if (isRebuildNeeded(indexCreator)) {
            rebuildIndex(indexCreator, REBUILD_SOAKING_PERIOD);
          } else {
            indexCreator.create();
            created++;
          }
        } catch (MongoCommandException mex) {
          // 86 - Index must have unique name.
          if (mex.getErrorCode() == 85 || mex.getErrorCode() == 86) {
            rebuildIndex(indexCreator, REBUILD_SOAKING_PERIOD);
          } else {
            logger.error("Failed to create index", mex);
          }
        }
      } catch (DuplicateKeyException exception) {
        logger.error("Because of deployment, a new index with uniqueness flag was introduced. "
                + "Current data does not meet this expectation."
                + "Create a migration to align the data with expectation or delete the uniqueness criteria from index",
            exception);
      } catch (RuntimeException exception) {
        logger.error("Unexpected exception when trying to create index", exception);
      }
    }
    return created;
  }

  public static Map<String, IndexCreator> indexCreators(MappedClass mc, DBCollection collection) {
    Map<String, IndexCreator> creators = new HashMap<>();

    // Read Entity level "Indexes" annotation
    List<Indexes> indexesAnnotations = mc.getAnnotations(Indexes.class);
    if (indexesAnnotations != null) {
      indexesAnnotations.stream().flatMap(indexes -> Arrays.stream(indexes.value())).forEach(index -> {
        BasicDBObject keys = new BasicDBObject();
        for (Field field : index.fields()) {
          keys.append(field.value(), field.type().toIndexValue());
        }

        String indexName = index.options().name();
        if (isEmpty(indexName)) {
          logger.error("Do not use default index name. WARNING: this index will not be created!!!");
        } else {
          BasicDBObject options = new BasicDBObject();
          options.put(NAME, indexName);
          if (index.options().unique()) {
            options.put(UNIQUE, Boolean.TRUE);
          } else {
            options.put(BACKGROUND, Boolean.TRUE);
          }
          if (index.options().sparse()) {
            options.put(SPARSE, Boolean.TRUE);
          }
          creators.put(indexName, IndexCreator.builder().collection(collection).keys(keys).options(options).build());
        }
      });
    }

    // Read field level "Indexed" annotation
    for (MappedField mf : mc.getPersistenceFields()) {
      if (!mf.hasAnnotation(Indexed.class)) {
        continue;
      }

      Indexed indexed = mf.getAnnotation(Indexed.class);

      int direction = 1;
      String name = isNotEmpty(indexed.options().name()) ? indexed.options().name() : mf.getNameToStore();

      String indexName = name + "_" + direction;
      BasicDBObject dbObject = new BasicDBObject(name, direction);

      BasicDBObject options = new BasicDBObject();
      options.put(NAME, indexName);
      if (indexed.options().unique()) {
        options.put(UNIQUE, Boolean.TRUE);
      } else {
        options.put(BACKGROUND, Boolean.TRUE);
      }
      if (indexed.options().sparse()) {
        options.put(SPARSE, Boolean.TRUE);
      }
      if (indexed.options().expireAfterSeconds() != -1) {
        options.put(EXPIRE_AFTER_SECONDS, indexed.options().expireAfterSeconds());
      }

      creators.put(indexName, IndexCreator.builder().collection(collection).keys(dbObject).options(options).build());
    }

    return creators;
  }
}
