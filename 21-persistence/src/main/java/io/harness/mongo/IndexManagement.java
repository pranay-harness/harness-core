package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoCommandException;
import com.mongodb.ReadPreference;
import io.harness.exception.UnexpectedException;
import io.harness.mongo.SampleEntity.SampleEntityKeys;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IndexManagement {
  private static final Logger logger = LoggerFactory.getLogger(IndexManagement.class);

  private interface IndexCreator { void create(); }

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

      final String name = (String) object.get("name");
      final BasicDBObject accessesObject = (BasicDBObject) object.get("accesses");

      Accesses accesses =
          new Accesses(((Long) accessesObject.get("ops")).intValue(), (Date) accessesObject.get("since"));

      accessesMap.put(name, accesses);
    }
    return accessesMap;
  }

  private static Map<String, Accesses> mergeAccesses(
      Map<String, Accesses> accessesPrimary, Map<String, Accesses> accessesSecondary) {
    Map<String, Accesses> accessesMap = new HashMap<>();

    Set<String> indexes = new HashSet<>();
    indexes.addAll(accessesPrimary.keySet());
    indexes.addAll(accessesSecondary.keySet());

    for (String index : indexes) {
      if (!accessesPrimary.containsKey(index)) {
        accessesMap.put(index, accessesSecondary.get(index));
      } else if (!accessesSecondary.containsKey(index)) {
        accessesMap.put(index, accessesPrimary.get(index));
      } else {
        Accesses primary = accessesPrimary.get(index);
        Accesses secondary = accessesSecondary.get(index);

        accessesMap.put(index,
            new Accesses(primary.getOperations() + secondary.getOperations(),
                primary.getSince().before(secondary.getSince()) ? primary.getSince() : secondary.getSince()));
      }
    }
    return accessesMap;
  }

  // This for checks for unused indexes. It utilize the indexStat provided from mongo.
  // A good article on the topic:
  // https://www.objectrocket.com/blog/mongodb/considerations-for-using-indexstats-to-find-unused-indexes-in-mongodb/
  // NOTE: This is work in progress. For the time being we are checking only for completely unused indexes.
  private static void checkForUnusedIndexes(DBCollection collection) {
    final Map<String, Accesses> accessesPrimary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.primary()));
    final Map<String, Accesses> accessesSecondary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.secondary()));

    final Map<String, Accesses> accesses = mergeAccesses(accessesPrimary, accessesSecondary);

    final long now = System.currentTimeMillis();
    final Date tooNew = new Date(now - Duration.ofDays(7).toMillis());

    final Set<String> uniqueIndexes = collection.getIndexInfo()
                                          .stream()
                                          .filter(obj -> {
                                            final Object unique = obj.get("unique");
                                            return unique != null && unique.toString().equals("true");
                                          })
                                          .map(obj -> obj.get("name").toString())
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
          logger.info(
              format("Index %s.%s is not used at for %d days", collection.getName(), entry.getKey(), passed.toDays()));
        });
  }

  public static void ensureIndex(AdvancedDatastore primaryDatastore, Morphia morphia) {
    /*
    Morphia auto creates embedded/nested Entity indexes with the parent Entity indexes.
    There is no way to override this behavior.
    https://github.com/mongodb/morphia/issues/706
     */

    Set<String> processedCollections = new HashSet<>();

    morphia.getMapper().getMappedClasses().forEach(mc -> {
      if (mc.getEntityAnnotation() == null) {
        return;
      }

      final DBCollection collection = primaryDatastore.getCollection(mc.getClazz());
      if (processedCollections.contains(collection.getName())) {
        return;
      }
      processedCollections.add(collection.getName());

      Map<String, IndexCreator> creators = new HashMap<>();

      // Read Entity level "Indexes" annotation
      List<Indexes> indexesAnnotations = mc.getAnnotations(Indexes.class);
      if (indexesAnnotations != null) {
        indexesAnnotations.stream().flatMap(indexes -> Arrays.stream(indexes.value())).forEach(index -> {
          reportDeprecatedUnique(index);

          BasicDBObject keys = new BasicDBObject();
          for (Field field : index.fields()) {
            keys.append(field.value(), field.type().toIndexValue());
          }

          final String indexName = index.options().name();
          if (isEmpty(indexName)) {
            logger.error("Do not use default index name for collection: {}\n"
                    + "WARNING: this index will not be created",
                collection.getName());
          } else {
            DBObject options = new BasicDBObject();
            options.put("name", indexName);
            if (index.options().unique()) {
              options.put("unique", Boolean.TRUE);
            } else {
              options.put("background", Boolean.TRUE);
            }
            creators.put(indexName, () -> collection.createIndex(keys, options));
          }
        });
      }

      // Read field level "Indexed" annotation
      for (final MappedField mf : mc.getPersistenceFields()) {
        if (mf.hasAnnotation(Indexed.class)) {
          final Indexed indexed = mf.getAnnotation(Indexed.class);
          reportDeprecatedUnique(indexed);

          int direction = 1;
          final String name = isNotEmpty(indexed.options().name()) ? indexed.options().name() : mf.getNameToStore();

          final String indexName = name + "_" + direction;
          BasicDBObject dbObject = new BasicDBObject(name, direction);

          DBObject options = new BasicDBObject();
          options.put("name", indexName);
          if (indexed.options().unique()) {
            options.put("unique", Boolean.TRUE);
          } else {
            options.put("background", Boolean.TRUE);
          }
          if (indexed.options().expireAfterSeconds() != -1) {
            options.put("expireAfterSeconds", indexed.options().expireAfterSeconds());
          }

          creators.put(indexName, () -> collection.createIndex(dbObject, options));
        }
      }

      final List<String> obsoleteIndexes = collection.getIndexInfo()
                                               .stream()
                                               .map(obj -> obj.get("name").toString())
                                               .filter(name -> !"_id_".equals(name))
                                               .filter(name -> !creators.keySet().contains(name))
                                               .collect(toList());
      if (isNotEmpty(obsoleteIndexes)) {
        logger.error("Obsolete indexes: {} : {}", collection.getName(), Joiner.on(", ").join(obsoleteIndexes));
        obsoleteIndexes.forEach(name -> {
          try {
            collection.dropIndex(name);
          } catch (RuntimeException ex) {
            logger.error(format("Failed to drop index %s", name), ex);
          }
        });
      }

      creators.forEach((name, creator) -> {
        try {
          for (int retry = 0; retry < 2; ++retry) {
            try {
              creator.create();
              break;
            } catch (MongoCommandException mex) {
              // 86 - Index must have unique name.
              if (mex.getErrorCode() == 85 || mex.getErrorCode() == 86) {
                try {
                  logger.warn("Drop index: {}.{}", collection.getName(), name);
                  collection.dropIndex(name);
                } catch (RuntimeException ex) {
                  logger.error("Failed to drop index {}", name, mex);
                }
              } else {
                logger.error("Failed to create index {}", name, mex);
              }
            }
          }
        } catch (DuplicateKeyException exception) {
          logger.error(
              "Because of deployment, a new index with uniqueness flag was introduced. Current data does not meet this expectation."
                  + "Create a migration to align the data with expectation or delete the uniqueness criteria from index",
              exception);
        }
      });

      try {
        checkForUnusedIndexes(collection);
      } catch (Exception exception) {
        logger.warn("", exception);
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

    final List<String> obsoleteCollections = primaryDatastore.getDB()
                                                 .getCollectionNames()
                                                 .stream()
                                                 .filter(name -> !processedCollections.contains(name))
                                                 .filter(name -> !whitelistCollections.contains(name))
                                                 .filter(name -> !name.startsWith("!!!test"))
                                                 .collect(toList());

    if (isNotEmpty(obsoleteCollections)) {
      logger.error("Unknown mongo collections detected: {}\n"
              + "Please create migration to delete them or add them to the whitelist.",
          Joiner.on(", ").join(obsoleteCollections));
    }
  }

  @SuppressWarnings("deprecation")
  public static void reportDeprecatedUnique(final Index index) {
    if (index.unique()) {
      throw new UnexpectedException("Someone still uses deprecated unique annotation");
    }
  }

  @SuppressWarnings("deprecation")
  public static void reportDeprecatedUnique(final Indexed indexed) {
    if (indexed.unique()) {
      throw new UnexpectedException("Someone still uses deprecated unique annotation");
    }
  }
}
