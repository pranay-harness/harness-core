package io.harness.accesscontrol.principals;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.principals.usergroups.UserGroupServiceImpl;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDao;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDaoImpl;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupMorphiaRegistrar;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class PrincipalModule extends AbstractModule {
  private static PrincipalModule instance;

  public static synchronized PrincipalModule getInstance() {
    if (instance == null) {
      instance = new PrincipalModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});

    morphiaRegistrars.addBinding().toInstance(UserGroupMorphiaRegistrar.class);
    bind(UserGroupDao.class).to(UserGroupDaoImpl.class);
    bind(UserGroupService.class).to(UserGroupServiceImpl.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(RoleAssignmentService.class);
    requireBinding(TransactionTemplate.class);
  }
}
