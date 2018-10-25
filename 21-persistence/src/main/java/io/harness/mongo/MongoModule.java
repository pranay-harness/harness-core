package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.AggregationOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.ReadPreference;
import io.harness.exception.UnexpectedException;
import io.harness.logging.MorphiaLoggerFactory;
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

public class MongoModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(MongoModule.class);

  // Java packages which Morphia will map
  // Indexes will not be created for any class outside these.
  private static final List<String> JAVA_PACKAGES_TO_SCAN = Arrays.asList("software.wings", "io.harness");

  private AdvancedDatastore primaryDatastore;
  private AdvancedDatastore secondaryDatastore;
  private DistributedLockSvc distributedLockSvc;

  public static final MongoClientOptions.Builder mongoClientOptions =
      MongoClientOptions.builder()
          .retryWrites(false)
          // TODO: Using secondaryPreferred creates issues that need to be investigated
          //.readPreference(ReadPreference.secondaryPreferred())
          .connectTimeout(30000)
          .serverSelectionTimeout(90000)
          .maxConnectionIdleTime(600000)
          .connectionsPerHost(300);

  /**
   * Creates a guice module for portal app.
   *
   */
  public MongoModule(MongoConfig mongoConfig) {
    registerLogger(MorphiaLoggerFactory.class);

    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);

    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri(), mongoClientOptions);
    MongoClient mongoClient = new MongoClient(uri);

    primaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
    primaryDatastore.setQueryFactory(new QueryFactory());

    MongoClientURI locksUri = uri;
    MongoClient mongoLocksClient = mongoClient;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      locksUri = new MongoClientURI(mongoConfig.getLocksUri(), mongoClientOptions);
      mongoLocksClient = new MongoClient(locksUri);
    }

    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoLocksClient, locksUri.getDatabase(), "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    distributedLockSvc = new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc();

    if (uri.getHosts().size() > 1) {
      secondaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
      secondaryDatastore.setQueryFactory(new QueryFactory());
    } else {
      secondaryDatastore = primaryDatastore;
    }

    JAVA_PACKAGES_TO_SCAN.forEach(morphia::mapPackage);
    ensureIndex(morphia);
  }

  /**
   * Instantiates a new database module.
   *
   * @param primaryDatastore   the primary datastore
   * @param secondaryDatastore the secondary datastore
   * @param distributedLockSvc the distributed lock svc
   */
  public MongoModule(
      AdvancedDatastore primaryDatastore, AdvancedDatastore secondaryDatastore, DistributedLockSvc distributedLockSvc) {
    this.primaryDatastore = primaryDatastore;
    this.secondaryDatastore = secondaryDatastore;

    this.distributedLockSvc = distributedLockSvc;
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

  private interface IndexCreator { void create(); }

  @Value
  @AllArgsConstructor
  class Accesses {
    int operations;
    Date since;
  }

  private Map<String, Accesses> extractAccesses(Cursor cursor) {
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

  private Map<String, Accesses> mergeAccesses(
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
  private void checkForUnusedIndexes(DBCollection collection) {
    final Map<String, Accesses> accessesPrimary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.primary()));
    final Map<String, Accesses> accessesSecondary = extractAccesses(
        collection.aggregate(Arrays.<DBObject>asList(new BasicDBObject("$indexStats", new BasicDBObject())),
            AggregationOptions.builder().build(), ReadPreference.secondary()));

    final Map<String, Accesses> accesses = mergeAccesses(accessesPrimary, accessesSecondary);

    final long now = System.currentTimeMillis();
    final Date tooNew = new Date(now - Duration.ofDays(7).toMillis());

    accesses.entrySet()
        .stream()
        .filter(entry -> entry.getValue().getOperations() == 0)
        .filter(entry -> entry.getValue().getSince().compareTo(tooNew) < 0)
        // Exclude ttl indexes, Ttl monitoring is not tracked as operations
        .filter(entry -> !entry.getKey().startsWith("validUntil"))
        // Temporary exclude indexes that coming from Base class. Currently we have no flexibility to enable/disable
        // such for different objects.
        .filter(entry -> !entry.getKey().startsWith("appId"))
        .filter(entry -> !entry.getKey().startsWith("keywords"))
        .filter(entry -> !entry.getKey().startsWith("createdAt"))
        // Alert for every index that left:
        .forEach(entry -> {
          Duration passed = Duration.between(ZonedDateTime.now().toInstant(), entry.getValue().getSince().toInstant());
          logger.error(format("(work in progress alert): Index %s.%s is not used at for days %d", collection.getName(),
              entry.getKey(), passed.toDays()));
        });
  }

  private void ensureIndex(Morphia morphia) {
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
            keys.append(field.value(), 1);
          }

          final String indexName = index.options().name();
          if (isEmpty(indexName)) {
            logger.error("Do not use default index name for collection: {}\n"
                    + "WARNING: this index will not be created",
                collection.getName());
          } else {
            creators.put(indexName, () -> collection.createIndex(keys, indexName, index.options().unique()));
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

      checkForUnusedIndexes(collection);
    });

    Set<String> whitelistCollections = ImmutableSet.<String>of(
        // Files and chinks
        "artifacts.chunks", "artifacts.files", "audits.chunks", "audits.files", "configs.chunks", "configs.files",
        "platforms.chunks", "platforms.files", "terraform_state.chunks", "terraform_state.files",
        // Quartz
        "quartz_calendars", "quartz_jobs", "quartz_locks", "quartz_schedulers", "quartz_triggers",
        // Quartz Verification
        "quartz_verification_calendars", "quartz_verification_jobs", "quartz_verification_locks",
        "quartz_verification_schedulers", "quartz_verification_triggers",
        // Persistent locks
        "locks");

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

  /* (non-Javadoc)
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    bind(AdvancedDatastore.class).annotatedWith(Names.named("primaryDatastore")).toInstance(primaryDatastore);
    bind(AdvancedDatastore.class).annotatedWith(Names.named("secondaryDatastore")).toInstance(secondaryDatastore);
    bind(DistributedLockSvc.class).toInstance(distributedLockSvc);

    // TODO: this is should be enabled when all wingsPersistence functionality is promoted to MongoPersistence and the
    //       class is removed. Till then we are binding the HPersistence to the wingsPersistence instance.
    // bind(HPersistence.class).to(MongoPersistence.class);
  }

  /**
   * Gets primary datastore.
   *
   * @return the primary datastore
   */
  public AdvancedDatastore getPrimaryDatastore() {
    return primaryDatastore;
  }
}
