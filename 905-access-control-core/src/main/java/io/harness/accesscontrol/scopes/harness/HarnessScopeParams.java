package io.harness.accesscontrol.scopes.harness;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.accesscontrol.scopes.core.ScopeParams;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarnessScopeParams implements ScopeParams {
  public static final String ACCOUNT_LEVEL_PARAM_NAME = ACCOUNT_KEY;
  public static final String ORG_LEVEL_PARAM_NAME = ORG_KEY;
  public static final String PROJECT_LEVEL_PARAM_NAME = PROJECT_KEY;

  @NotEmpty @QueryParam(ACCOUNT_LEVEL_PARAM_NAME) String accountIdentifier;
  @QueryParam(ORG_LEVEL_PARAM_NAME) String orgIdentifier;
  @QueryParam(PROJECT_LEVEL_PARAM_NAME) String projectIdentifier;

  @Override
  public Map<String, String> getParams() {
    Map<String, String> params = new HashMap<>();
    params.put(ACCOUNT_LEVEL_PARAM_NAME, accountIdentifier);
    if (orgIdentifier == null) {
      return params;
    }
    params.put(ORG_LEVEL_PARAM_NAME, orgIdentifier);
    if (projectIdentifier == null) {
      return params;
    }
    params.put(PROJECT_LEVEL_PARAM_NAME, projectIdentifier);
    return params;
  }
}
