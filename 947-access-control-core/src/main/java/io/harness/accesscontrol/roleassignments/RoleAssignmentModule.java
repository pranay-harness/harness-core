/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.roleassignments;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDao;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDaoImpl;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentMorphiaRegistrar;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidator;
import io.harness.accesscontrol.roleassignments.validator.RoleAssignmentValidatorImpl;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(PL)
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
    requireBinding(RoleService.class);
    requireBinding(ResourceGroupService.class);
    requireBinding(MongoTemplate.class);
  }
}
