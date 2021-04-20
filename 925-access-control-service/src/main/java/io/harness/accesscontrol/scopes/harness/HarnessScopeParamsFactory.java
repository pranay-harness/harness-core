package io.harness.accesscontrol.scopes.harness;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ACCOUNT_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.ORG_LEVEL_PARAM_NAME;
import static io.harness.accesscontrol.scopes.harness.HarnessScopeParams.PROJECT_LEVEL_PARAM_NAME;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeParams;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(PL)
@Singleton
public class HarnessScopeParamsFactory implements ScopeParamsFactory {
  @Override
  public ScopeParams buildScopeParams(Scope scope) {
    return buildScopeParamsFromScope(scope);
  }

  public static HarnessScopeParams buildScopeParamsFromScope(Scope scope) {
    Map<String, String> params = new HashMap<>();
    Scope currentScope = scope;
    while (currentScope != null) {
      params.put(currentScope.getLevel().getParamName(), currentScope.getInstanceId());
      currentScope = currentScope.getParentScope();
    }
    return HarnessScopeParams.builder()
        .accountIdentifier(params.get(ACCOUNT_LEVEL_PARAM_NAME))
        .orgIdentifier(params.get(ORG_LEVEL_PARAM_NAME))
        .projectIdentifier(params.get(PROJECT_LEVEL_PARAM_NAME))
        .build();
  }
}
