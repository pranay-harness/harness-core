package io.harness.accesscontrol.scopes;

import static io.harness.accesscontrol.scopes.ScopeServiceImpl.SCOPES_BY_IDENTIFIER_KEY;
import static io.harness.accesscontrol.scopes.ScopeServiceImpl.SCOPES_BY_PATH_KEY;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

public class ScopeModule extends AbstractModule {
  private static ScopeModule instance;

  public static ScopeModule getInstance() {
    if (instance == null) {
      instance = new ScopeModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ScopeService.class).to(ScopeServiceImpl.class);

    MapBinder<String, Scope> scopesByIdentifierKey =
        MapBinder.newMapBinder(binder(), String.class, Scope.class, Names.named(SCOPES_BY_IDENTIFIER_KEY));
    scopesByIdentifierKey.addBinding(HarnessScope.ACCOUNT.getIdentifierKey()).toInstance(HarnessScope.ACCOUNT);
    scopesByIdentifierKey.addBinding(HarnessScope.ORGANIZATION.getIdentifierKey())
        .toInstance(HarnessScope.ORGANIZATION);
    scopesByIdentifierKey.addBinding(HarnessScope.PROJECT.getIdentifierKey()).toInstance(HarnessScope.PROJECT);

    MapBinder<String, Scope> scopesByPathKey =
        MapBinder.newMapBinder(binder(), String.class, Scope.class, Names.named(SCOPES_BY_PATH_KEY));
    scopesByPathKey.addBinding(HarnessScope.ACCOUNT.getIdentifierKey()).toInstance(HarnessScope.ACCOUNT);
    scopesByPathKey.addBinding(HarnessScope.ORGANIZATION.getIdentifierKey()).toInstance(HarnessScope.ORGANIZATION);
    scopesByPathKey.addBinding(HarnessScope.PROJECT.getIdentifierKey()).toInstance(HarnessScope.PROJECT);
  }
}
