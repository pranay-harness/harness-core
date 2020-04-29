package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.IndexManagerCollectionSession.createCollectionSession;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofHours;
import static java.util.stream.Collectors.toList;

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
import io.harness.govern.Switch;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.IndexManager.IndexCreator;
import io.harness.threading.Morpheus;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.MappedField;

import java.time.Duration;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class IndexManagerSession {
  public static final String UNIQUE = "unique";
  public static final String BACKGROUND = "background";
  public static final String SPARSE = "sparse";
  public static final String NAME = "name";
  public static final String EXPIRE_AFTER_SECONDS = "expireAfterSeconds";
  public static final Duration SOAKING_PERIOD = ofDays(1);
  // We do not want to drop temporary index before we created the new one. Make the soaking period smaller.
  public static final Duration REBUILD_SOAKING_PERIOD = SOAKING_PERIOD.minus(ofHours(1));

  private IndexManager.Mode mode;

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

  public static Map<String, Accesses> fetchIndexAccesses(DBCollection collection) {
    Map<String, Accesses> accessesPrimary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.primary()));
    Map<String, Accesses> accessesSecondary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.secondary()));

    return mergeAccesses(accessesPrimary, accessesSecondary);
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

  static Date tooNew() {
    return new Date(currentTimeMillis() - SOAKING_PERIOD.toMillis());
  }

  public static Map<String, IndexCreator> indexCreators(MappedClass mc, DBCollection collection) {
    Map<String, IndexCreator> creators = new HashMap<>();

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

    // Read Entity level "Indexes" annotation
    List<Indexes> indexesAnnotations = mc.getAnnotations(Indexes.class);
    if (indexesAnnotations != null) {
      indexesAnnotations.stream().flatMap(indexes -> Arrays.stream(indexes.value())).forEach(index -> {
        BasicDBObject keys = new BasicDBObject();

        if (index.fields().length == 1 && !index.fields()[0].value().contains(".")) {
          logger.error("Composite index with only one field {}", index.fields()[0].value());
        }

        for (Field field : index.fields()) {
          keys.append(field.value(), field.type().toIndexValue());
        }

        String indexName = index.options().name();
        if (isEmpty(indexName)) {
          logger.error("Do not use default index name. WARNING: this index will not be created!!!");
          return;
        }
        if (creators.containsKey(indexName)) {
          // Java doubles some annotations
          return;
        }

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

        IndexCreator newCreator = IndexCreator.builder().collection(collection).keys(keys).options(options).build();

        for (IndexCreator creator : creators.values()) {
          if (creator.sameKeySet(newCreator)) {
            logger.error("Index {} and {} have the same set of keys", newCreator.getOptions().toString(),
                creator.getOptions().toString());
          }
        }

        creators.put(indexName, newCreator);
      });
    }

    return creators;
  }

  IndexManagerSession(IndexManager.Mode mode) {
    this.mode = mode;
  }

  public void create(IndexCreator indexCreator) {
    switch (mode) {
      case AUTO:
        logger.warn("Creating index {} {}", indexCreator.getOptions().toString(), indexCreator.getKeys().toString());
        for (int i = 0; i < 10; i++) {
          try {
            indexCreator.getCollection().createIndex(indexCreator.getKeys(), indexCreator.getOptions());
            break;
          } catch (MongoCommandException exception) {
            // Creating too many indexes at the same time might overwhelm mongo. Give it some time to catch up.
            if (exception.getErrorCode() == 24) {
              Morpheus.sleep(Duration.ofSeconds(1));
            } else {
              throw exception;
            }
          }
        }
        break;
      case MANUAL:
        logger.info(
            "Should create index {} {}\n{}", indexCreator.getOptions().toString(), indexCreator.getKeys().toString());
        break;
      case INSPECT:
        String stats = "Empty collection";
        if (indexCreator.getCollection().count() > 0) {
          stats = indexCreator.getCollection().getStats().toString();
        }
        logger.error("Should create index {}\nScript: db.{}.createIndex({}, {})\n{}",
            indexCreator.getOptions().get(NAME), indexCreator.getCollection().getName(),
            indexCreator.getKeys().toString(), indexCreator.getOptions().toString(), stats);
        break;
      default:
        Switch.unhandled(mode);
    }
  }

  public void dropIndex(DBCollection collection, String indexName) {
    switch (mode) {
      case AUTO:
        logger.warn("Dropping index {}", indexName);
        collection.dropIndex(indexName);
        break;
      case MANUAL:
        logger.info("Should drop index {}", indexName);
        break;
      case INSPECT:
        logger.error("Should drop index {}\nScript: db.{}.dropIndex('{}')", indexName, collection.getName(), indexName);
        break;
      default:
        Switch.unhandled(mode);
    }
  }

  public boolean rebuildIndex(
      IndexManagerCollectionSession collectionSession, IndexCreator indexCreator, Duration waitFor) {
    DBObject indexByFields = collectionSession.findIndexByFields(indexCreator);
    if (indexByFields == null) {
      return false;
    }

    String currentName = (String) indexByFields.get(NAME);

    // Mongo does not support index updates - we have to recreate the index

    // We do not want to have to make queries without an index. We cannot create another one though, because mongo
    // will detect that we already have such with the same set of fields.
    // We are going to add an extra dummy field. this is safe because fields at the end will not prevent the index to
    // be used for other purposes.
    BasicDBObject tempKeys = (BasicDBObject) indexCreator.getKeys().copy();
    tempKeys.put("very_dummy_field", 1);

    // We also have to pick another name
    BasicDBObject tempOptions = (BasicDBObject) indexCreator.getOptions().copy();
    tempOptions.put(NAME, "TRI_" + currentTimeMillis());

    IndexCreator tempCreator =
        IndexCreator.builder().collection(indexCreator.getCollection()).keys(tempKeys).options(tempOptions).build();

    // Lets see if we already have this one created from before
    DBObject triIndex = collectionSession.findIndexByFields(tempCreator);
    if (triIndex == null) {
      create(tempCreator);
      collectionSession.reset(tempCreator.getCollection());
      triIndex = collectionSession.findIndexByFields(tempCreator);
      if (triIndex == null) {
        return true;
      }
    }

    // Check if the temporary index soaked enough
    long indexTime = Long.parseLong(triIndex.get(NAME).toString().substring(4));
    if (indexTime + waitFor.toMillis() > currentTimeMillis()) {
      return false;
    }

    // now we are safe to drop the original
    dropIndex(indexCreator.getCollection(), currentName);

    // Lets create the target index
    create(indexCreator);
    return true;
  }

  int createNewIndexes(IndexManagerCollectionSession collectionSession, Map<String, IndexCreator> creators) {
    int created = 0;
    for (Entry<String, IndexCreator> creator : creators.entrySet()) {
      String name = creator.getKey();
      try (AutoLogContext ignore = new IndexLogContext(name, OVERRIDE_ERROR)) {
        IndexCreator indexCreator = creator.getValue();
        try {
          if (collectionSession.isRebuildNeeded(indexCreator)) {
            if (rebuildIndex(collectionSession, indexCreator, REBUILD_SOAKING_PERIOD)) {
              created++;
            }
          } else if (collectionSession.isCreateNeeded(indexCreator)) {
            create(indexCreator);
            created++;
          }
        } catch (MongoCommandException mex) {
          // 86 - Index must have unique name.
          if (mex.getErrorCode() == 85 || mex.getErrorCode() == 86) {
            if (rebuildIndex(collectionSession, indexCreator, REBUILD_SOAKING_PERIOD)) {
              created++;
            }
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

  public boolean ensureIndexes(AdvancedDatastore datastore, Morphia morphia) {
    // Morphia auto creates embedded/nested Entity indexes with the parent Entity indexes.
    // There is no way to override this behavior.
    // https://github.com/mongodb/morphia/issues/706

    AtomicBoolean actionPerformed = new AtomicBoolean(false);

    Set<String> processedCollections = processIndexes(datastore, morphia, (mc, collection) -> {
      try {
        Map<String, IndexCreator> creators = indexCreators(mc, collection);
        // We should be attempting to drop indexes only if we successfully created all new ones
        int created = createNewIndexes(createCollectionSession(collection), creators);
        if (created > 0) {
          actionPerformed.set(true);
        }

        boolean okToDropIndexes = created == 0;

        Map<String, Accesses> accesses = fetchIndexAccesses(collection);

        if (okToDropIndexes) {
          IndexManagerCollectionSession collectionSession = createCollectionSession(collection);
          List<String> obsoleteIndexes = collectionSession.obsoleteIndexes(creators.keySet());
          if (isNotEmpty(obsoleteIndexes)) {
            // Make sure that all indexes that we have are operational, we check that they have being seen since
            // at least a day
            Date tooNew = tooNew();
            okToDropIndexes = collectionSession.isOkToDropIndexes(tooNew, accesses);

            if (okToDropIndexes) {
              obsoleteIndexes.forEach(name -> {
                try (AutoLogContext ignore2 = new IndexLogContext(name, OVERRIDE_ERROR)) {
                  dropIndex(collection, name);
                  actionPerformed.set(true);
                } catch (RuntimeException ex) {
                  logger.error("Failed to drop index", ex);
                }
              });
            }
          }
        }

        if (mc.getClazz().getAnnotation(IgnoreUnusedIndex.class) == null) {
          createCollectionSession(collection).checkForUnusedIndexes(accesses);
        }
      } catch (MongoCommandException exception) {
        if (exception.getErrorCode() == 13) {
          throw new IndexManagerReadOnlyException();
        }
        logger.error("", exception);
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

    return actionPerformed.get();
  }
}
