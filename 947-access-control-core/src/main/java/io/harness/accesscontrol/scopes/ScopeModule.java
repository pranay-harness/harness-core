package io.harness.accesscontrol.scopes;

import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.core.ScopeServiceImpl;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
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
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(ScopeParamsFactory.class);
    requireBinding(Key.get(new TypeLiteral<Map<String, ScopeLevel>>() {}));
  }
}
