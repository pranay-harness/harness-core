package io.harness.accesscontrol.scopes;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ACCOUNT;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.ORGANIZATION;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeLevel.PROJECT;

import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.core.ScopeServiceImpl;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParamsFactory;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

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

    MapBinder<String, ScopeLevel> scopesByKey = MapBinder.newMapBinder(binder(), String.class, ScopeLevel.class);
    scopesByKey.addBinding(ACCOUNT.toString()).toInstance(ACCOUNT);
    scopesByKey.addBinding(ORGANIZATION.toString()).toInstance(ORGANIZATION);
    scopesByKey.addBinding(PROJECT.toString()).toInstance(PROJECT);

    bind(ScopeParamsFactory.class).to(HarnessScopeParamsFactory.class);
  }
}
