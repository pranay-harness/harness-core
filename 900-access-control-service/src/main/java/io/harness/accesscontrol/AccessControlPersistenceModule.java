package io.harness.accesscontrol;

import io.harness.accesscontrol.acl.ACLPersistenceConfig;
import io.harness.accesscontrol.permissions.persistence.PermissionPersistenceConfig;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupPersistenceConfig;
import io.harness.accesscontrol.resources.resourcetypes.persistence.ResourceTypePersistenceConfig;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentPersistenceConfig;
import io.harness.accesscontrol.roles.persistence.RolePersistenceConfig;
import io.harness.aggregator.AggregatorPersistenceConfig;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.queue.QueueController;
import io.harness.serializer.KryoRegistrar;
import io.harness.springdata.HTransactionTemplate;
import io.harness.springdata.PersistenceModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class AccessControlPersistenceModule extends PersistenceModule {
  private static AccessControlPersistenceModule instance;
  private final MongoConfig mongoConfig;

  private AccessControlPersistenceModule(MongoConfig mongoConfig) {
    this.mongoConfig = mongoConfig;
  }

  public static synchronized AccessControlPersistenceModule getInstance(MongoConfig mongoConfig) {
    if (instance == null) {
      instance = new AccessControlPersistenceModule(mongoConfig);
    }
    return instance;
  }

  @Provides
  @Singleton
  MongoConfig mongoConfig() {
    return mongoConfig;
  }

  @Provides
  @Singleton
  protected TransactionTemplate getTransactionTemplate(
      MongoTransactionManager mongoTransactionManager, MongoConfig mongoConfig) {
    return new HTransactionTemplate(mongoTransactionManager, mongoConfig.isTransactionsEnabled());
  }

  @Override
  public void configure() {
    super.configure();
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    install(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
    Multibinder<Class<? extends KryoRegistrar>> kryoRegistrar =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends KryoRegistrar>>() {});
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    Multibinder<Class<? extends TypeConverter>> morphiaConverters =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends TypeConverter>>() {});
    MapBinder<Class, String> morphiaClasses = MapBinder.newMapBinder(
        binder(), new TypeLiteral<Class>() {}, new TypeLiteral<String>() {}, Names.named("morphiaClasses"));
    bind(HPersistence.class).to(MongoPersistence.class);
    registerRequiredBindings();
  }

  @Override
  protected Class<?>[] getConfigClasses() {
    return new Class[] {ResourceTypePersistenceConfig.class, ResourceGroupPersistenceConfig.class,
        PermissionPersistenceConfig.class, RolePersistenceConfig.class, RoleAssignmentPersistenceConfig.class,
        ACLPersistenceConfig.class, AggregatorPersistenceConfig.class};
  }

  private void registerRequiredBindings() {
    requireBinding(MongoConfig.class);
  }
}
