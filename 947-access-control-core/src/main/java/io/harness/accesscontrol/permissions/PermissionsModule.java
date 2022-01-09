/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.permissions;

import io.harness.accesscontrol.permissions.persistence.PermissionDao;
import io.harness.accesscontrol.permissions.persistence.PermissionDaoImpl;
import io.harness.accesscontrol.permissions.persistence.PermissionMorphiaRegistrar;
import io.harness.accesscontrol.resources.resourcetypes.ResourceTypeService;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.PL)
public class PermissionsModule extends AbstractModule {
  private static PermissionsModule instance;

  public static synchronized PermissionsModule getInstance() {
    if (instance == null) {
      instance = new PermissionsModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(PermissionMorphiaRegistrar.class);

    bind(PermissionService.class).to(PermissionServiceImpl.class);
    bind(PermissionDao.class).to(PermissionDaoImpl.class);
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeService.class);
    requireBinding(ResourceTypeService.class);
    requireBinding(RoleService.class);
    requireBinding(TransactionTemplate.class);
    requireBinding(MongoTemplate.class);
  }
}
