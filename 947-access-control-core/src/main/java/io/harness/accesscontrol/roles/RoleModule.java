/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roles;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.permissions.PermissionService;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roles.persistence.RoleDao;
import io.harness.accesscontrol.roles.persistence.RoleDaoImpl;
import io.harness.accesscontrol.roles.persistence.RoleMorphiaRegistrar;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(PL)
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
    requireBinding(PermissionService.class);
    requireBinding(RoleAssignmentService.class);
    requireBinding(ScopeService.class);
    requireBinding(MongoTemplate.class);
  }
}
