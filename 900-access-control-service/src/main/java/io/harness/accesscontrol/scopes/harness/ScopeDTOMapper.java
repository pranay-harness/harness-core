package io.harness.accesscontrol.scopes.harness;

import static io.harness.accesscontrol.scopes.harness.HarnessScopeParamsFactory.buildScopeParamsFromScope;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.scopes.ScopeDTO;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class ScopeDTOMapper {
  public ScopeDTO toDTO(Scope scope) {
    if (scope == null) {
      return null;
    }
    HarnessScopeParams harnessScopeParams = buildScopeParamsFromScope(scope);
    return ScopeDTO.builder()
        .accountIdentifier(harnessScopeParams.getAccountIdentifier())
        .orgIdentifier(harnessScopeParams.getOrgIdentifier())
        .projectIdentifier(harnessScopeParams.getProjectIdentifier())
        .build();
  }
}
