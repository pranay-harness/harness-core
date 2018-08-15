package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import io.harness.exception.UnexpectedException;
import io.harness.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ReadPref;
import software.wings.dl.MongoConfig;
import software.wings.lock.ManagedDistributedLockSvc;
import software.wings.utils.NoDefaultConstructorMorphiaObjectFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatabaseModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseModule.class);
  private AdvancedDatastore primaryDatastore;
  private AdvancedDatastore secondaryDatastore;
  private DistributedLockSvc distributedLockSvc;
  private Map<ReadPref, AdvancedDatastore> datastoreMap = Maps.newHashMap();

  public static final MongoClientOptions.Builder mongoClientOptions =
      MongoClientOptions.builder()
          .retryWrites(true)
          // TODO: Using secondaryPreferred creates issues that need to be investigated
          //.readPreference(ReadPreference.secondaryPreferred())
          .connectTimeout(30000)
          .serverSelectionTimeout(90000)
          .maxConnectionIdleTime(600000)
          .connectionsPerHost(300);

  /**
   * Creates a guice module for portal app.
   *
   * @param configuration DropWizard configuration
   */
  public DatabaseModule(MainConfiguration configuration) {
    registerLogger(MorphiaLoggerFactory.class);

    MongoConfig mongoConfig = configuration.getMongoConnectionFactory();
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);

    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri(), mongoClientOptions);
    MongoClient mongoClient = new MongoClient(uri);

    this.primaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
    DistributedLockSvcOptions distributedLockSvcOptions =
        new DistributedLockSvcOptions(mongoClient, uri.getDatabase(), "locks");
    distributedLockSvcOptions.setEnableHistory(false);
    distributedLockSvc =
        new ManagedDistributedLockSvc(new DistributedLockSvcFactory(distributedLockSvcOptions).getLockSvc());

    if (uri.getHosts().size() > 1) {
      this.secondaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
    } else {
      this.secondaryDatastore = primaryDatastore;
    }

    morphia.mapPackage("software.wings");
    ensureIndex(morphia);

    this.primaryDatastore.setQueryFactory(new HQueryFactory());
    this.secondaryDatastore.setQueryFactory(new HQueryFactory());
    datastoreMap.put(ReadPref.CRITICAL, primaryDatastore);
    datastoreMap.put(ReadPref.NORMAL, secondaryDatastore);
  }

  /**
   * Instantiates a new database module.
   *
   * @param primaryDatastore   the primary datastore
   * @param secondaryDatastore the secondary datastore
   * @param distributedLockSvc the distributed lock svc
   */
  public DatabaseModule(
      AdvancedDatastore primaryDatastore, AdvancedDatastore secondaryDatastore, DistributedLockSvc distributedLockSvc) {
    this.primaryDatastore = primaryDatastore;
    this.secondaryDatastore = secondaryDatastore;

    datastoreMap.put(ReadPref.CRITICAL, primaryDatastore);
    datastoreMap.put(ReadPref.NORMAL, secondaryDatastore);
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
            logger.error("Failed to drop index {}", name, ex);
          }
        });
      }

      creators.forEach((name, creator) -> {
        for (int retry = 0; retry < 2; ++retry) {
          try {
            creator.create();
            break;
          } catch (MongoCommandException mex) {
            if (mex.getErrorCode() == 85) {
              try {
                logger.warn("Drop index: {}.{}", collection.getName(), name);
                collection.dropIndex(name);
              } catch (RuntimeException ex) {
                logger.error("Failed to drop index {}", name, mex);
              }
            } else {
              logger.error("Failed to create index {}", name, mex);
            }
          } catch (DuplicateKeyException exception) {
            logger.error(
                "Because of deployment, a new index with uniqueness flag was introduced. Current data does not meet this expectation."
                    + "Create a migration to align the data with expectation or delete the uniqueness criteria from index",
                exception);
          }
        }
      });
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
    bind(new TypeLiteral<Map<ReadPref, AdvancedDatastore>>() {})
        .annotatedWith(Names.named("datastoreMap"))
        .toInstance(datastoreMap);
    bind(DistributedLockSvc.class).toInstance(distributedLockSvc);
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
