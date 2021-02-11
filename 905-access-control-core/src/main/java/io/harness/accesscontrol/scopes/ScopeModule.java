package io.harness.accesscontrol.scopes;

import static io.harness.accesscontrol.scopes.ScopeServiceImpl.SCOPES_BY_IDENTIFIER_NAME;
import static io.harness.accesscontrol.scopes.ScopeServiceImpl.SCOPES_BY_KEY;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

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

    MapBinder<String, Scope> scopesByIdentifierName =
        MapBinder.newMapBinder(binder(), String.class, Scope.class, Names.named(SCOPES_BY_IDENTIFIER_NAME));
    scopesByIdentifierName.addBinding(HarnessScope.ACCOUNT.getIdentifierName()).toInstance(HarnessScope.ACCOUNT);
    scopesByIdentifierName.addBinding(HarnessScope.ORGANIZATION.getIdentifierName())
        .toInstance(HarnessScope.ORGANIZATION);
    scopesByIdentifierName.addBinding(HarnessScope.PROJECT.getIdentifierName()).toInstance(HarnessScope.PROJECT);

    MapBinder<String, Scope> scopesByKey =
        MapBinder.newMapBinder(binder(), String.class, Scope.class, Names.named(SCOPES_BY_KEY));
    scopesByKey.addBinding(HarnessScope.ACCOUNT.getIdentifierName()).toInstance(HarnessScope.ACCOUNT);
    scopesByKey.addBinding(HarnessScope.ORGANIZATION.getIdentifierName()).toInstance(HarnessScope.ORGANIZATION);
    scopesByKey.addBinding(HarnessScope.PROJECT.getIdentifierName()).toInstance(HarnessScope.PROJECT);
  }
}
