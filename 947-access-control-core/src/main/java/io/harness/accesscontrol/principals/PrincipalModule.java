/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccountService;
import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccountServiceImpl;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDao;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDaoImpl;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountMorphiaRegistrar;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.principals.usergroups.UserGroupServiceImpl;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDao;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDaoImpl;
import io.harness.accesscontrol.principals.usergroups.persistence.UserGroupMorphiaRegistrar;
import io.harness.accesscontrol.principals.users.UserService;
import io.harness.accesscontrol.principals.users.UserServiceImpl;
import io.harness.accesscontrol.principals.users.persistence.UserDao;
import io.harness.accesscontrol.principals.users.persistence.UserDaoImpl;
import io.harness.accesscontrol.principals.users.persistence.UserMorphiaRegistrar;
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

    morphiaRegistrars.addBinding().toInstance(UserMorphiaRegistrar.class);
    bind(UserDao.class).to(UserDaoImpl.class);
    bind(UserService.class).to(UserServiceImpl.class);

    morphiaRegistrars.addBinding().toInstance(ServiceAccountMorphiaRegistrar.class);
    bind(ServiceAccountDao.class).to(ServiceAccountDaoImpl.class);
    bind(ServiceAccountService.class).to(ServiceAccountServiceImpl.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(RoleAssignmentService.class);
    requireBinding(TransactionTemplate.class);
  }
}
