package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.persistence.PermissionPersistenceConfig;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentPersistenceConfig;
import io.harness.accesscontrol.roles.persistence.RolePersistenceConfig;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.serializer.KryoRegistrar;
import io.harness.springdata.PersistenceModule;

import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import org.mongodb.morphia.converters.TypeConverter;

public class AccessControlPersistenceModule extends PersistenceModule {
  private static AccessControlPersistenceModule instance;

  public static synchronized AccessControlPersistenceModule getInstance() {
    if (instance == null) {
      instance = new AccessControlPersistenceModule();
    }
    return instance;
  }

  @Override
  public void configure() {
    super.configure();
    install(MongoModule.getInstance());
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
    return new Class[] {
        PermissionPersistenceConfig.class, RolePersistenceConfig.class, RoleAssignmentPersistenceConfig.class};
  }

  private void registerRequiredBindings() {
    requireBinding(MongoConfig.class);
  }
}
