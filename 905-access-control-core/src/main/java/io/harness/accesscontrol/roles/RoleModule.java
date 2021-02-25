package io.harness.accesscontrol.roles;

import io.harness.accesscontrol.roles.persistence.RoleDao;
import io.harness.accesscontrol.roles.persistence.RoleDaoImpl;
import io.harness.accesscontrol.roles.persistence.RoleMorphiaRegistrar;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.springframework.data.mongodb.core.MongoTemplate;

public class RoleModule extends AbstractModule {
  private static RoleModule instance;

  public static synchronized RoleModule getInstance() {
    if (instance == null) {
      instance = new RoleModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(RoleMorphiaRegistrar.class);

    bind(RoleService.class).to(RoleServiceImpl.class);
    bind(RoleDao.class).to(RoleDaoImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeService.class);
    requireBinding(MongoTemplate.class);
  }
}
