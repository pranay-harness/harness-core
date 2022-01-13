/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.scopes;

import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.core.ScopeServiceImpl;
import io.harness.accesscontrol.scopes.core.persistence.ScopeDao;
import io.harness.accesscontrol.scopes.core.persistence.ScopeDaoImpl;
import io.harness.accesscontrol.scopes.core.persistence.ScopeMorphiaRegistrar;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import java.util.Map;

@OwnedBy(HarnessTeam.PL)
public class ScopeModule extends AbstractModule {
  private static ScopeModule instance;

  public static synchronized ScopeModule getInstance() {
    if (instance == null) {
      instance = new ScopeModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ScopeService.class).to(ScopeServiceImpl.class);
    bind(ScopeDao.class).to(ScopeDaoImpl.class);

    Multibinder<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
        Multibinder.newSetBinder(binder(), new TypeLiteral<Class<? extends MorphiaRegistrar>>() {});
    morphiaRegistrars.addBinding().toInstance(ScopeMorphiaRegistrar.class);

    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(Key.get(new TypeLiteral<Map<String, ScopeLevel>>() {}));
  }
}
