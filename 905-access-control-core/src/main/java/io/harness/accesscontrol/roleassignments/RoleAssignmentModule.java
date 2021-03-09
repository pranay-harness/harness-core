package io.harness.accesscontrol.roleassignments;

import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDao;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDaoImpl;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentMorphiaRegistrar;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidator;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidatorImpl;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.springframework.data.mongodb.core.MongoTemplate;

public class RoleAssignmentModule extends AbstractModule {
  private static RoleAssignmentModule instance;

  public static synchronized RoleAssignmentModule getInstance() {
    if (instance == null) {
      instance = new RoleAssignmentModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(RoleAssignmentMorphiaRegistrar.class);

    bind(RoleAssignmentService.class).to(RoleAssignmentServiceImpl.class);
    bind(RoleAssignmentValidator.class).to(RoleAssignmentValidatorImpl.class);
    bind(RoleAssignmentDao.class).to(RoleAssignmentDaoImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeService.class);
    requireBinding(MongoTemplate.class);
  }
}
