package software.wings.app;

import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.deftlabs.lock.mongo.DistributedLockSvcFactory;
import com.deftlabs.lock.mongo.DistributedLockSvcOptions;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
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
import java.util.List;
import java.util.Map;

public class DatabaseModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseModule.class);
  private AdvancedDatastore primaryDatastore;
  private AdvancedDatastore secondaryDatastore;
  private DistributedLockSvc distributedLockSvc;
  private Map<ReadPref, AdvancedDatastore> datastoreMap = Maps.newHashMap();

  /**
   * Creates a guice module for portal app.
   *
   * @param configuration Dropwizard configuration
   */
  public DatabaseModule(MainConfiguration configuration) {
    registerLogger(MorphiaLoggerFactory.class);

    MongoConfig mongoConfig = configuration.getMongoConnectionFactory();
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);

    Builder mongoClientOptions = MongoClientOptions.builder()
                                     .retryWrites(true)
                                     .connectTimeout(30000)
                                     .serverSelectionTimeout(90000)
                                     .maxConnectionIdleTime(600000)
                                     .connectionsPerHost(300);
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

  private void ensureIndex(Morphia morphia) {
    /*
    Morphia auto creates embedded/nested Entity indexes with the parent Entity indexes.
    There is no way to override this behavior.
    https://github.com/mongodb/morphia/issues/706
     */

    morphia.getMapper().getMappedClasses().forEach(mc -> {
      if (mc.getEntityAnnotation() != null && !mc.isAbstract()) {
        // Read Entity level "Indexes" annotation
        List<Indexes> indexesAnnotations = mc.getAnnotations(Indexes.class);
        if (indexesAnnotations != null) {
          indexesAnnotations.stream().flatMap(indexes -> Arrays.stream(indexes.value())).forEach(index -> {
            reportDeprecatedUnique(index);

            BasicDBObject keys = new BasicDBObject();
            for (Field field : index.fields()) {
              keys.append(field.value(), 1);
            }
            this.primaryDatastore.getCollection(mc.getClazz())
                .createIndex(keys, index.options().name(), index.options().unique());
          });
        }

        // Read field level "Indexed" annotation
        for (final MappedField mf : mc.getPersistenceFields()) {
          if (mf.hasAnnotation(Indexed.class)) {
            final Indexed indexed = mf.getAnnotation(Indexed.class);
            reportDeprecatedUnique(indexed);

            BasicDBObject dbObject = new BasicDBObject().append(mf.getNameToStore(), 1);

            DBObject options = new BasicDBObject();
            if (indexed.options().unique()) {
              options.put("unique", Boolean.TRUE);
            }
            if (indexed.options().expireAfterSeconds() != -1) {
              options.put("expireAfterSeconds", indexed.options().expireAfterSeconds());
            }

            try {
              primaryDatastore.getCollection(mc.getClazz()).createIndex(dbObject, options);
            } catch (MongoCommandException mex) {
              if (mex.getErrorCode() != 85) {
                throw mex;
              }

              // When Index creation fails due to changed options drop it and recreate.
              primaryDatastore.getCollection(mc.getClazz())
                  .dropIndex(new BasicDBObject().append(mf.getNameToStore(), 1));
              primaryDatastore.getCollection(mc.getClazz()).createIndex(dbObject, options);
            }
          }
        }
      }
    });
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
