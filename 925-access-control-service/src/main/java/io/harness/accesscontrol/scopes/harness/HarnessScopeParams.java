package io.harness.accesscontrol.scopes.harness;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.Parameter;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HarnessScopeParams {
  public static final String ACCOUNT_LEVEL_PARAM_NAME = ACCOUNT_KEY;
  public static final String ORG_LEVEL_PARAM_NAME = ORG_KEY;
  public static final String PROJECT_LEVEL_PARAM_NAME = PROJECT_KEY;

  @Parameter(description = ACCOUNT_PARAM_MESSAGE)
  @NotEmpty
  @QueryParam(ACCOUNT_LEVEL_PARAM_NAME)
  private String accountIdentifier;
  @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(ORG_LEVEL_PARAM_NAME) private String orgIdentifier;
  @Parameter(description = PROJECT_PARAM_MESSAGE)
  @QueryParam(PROJECT_LEVEL_PARAM_NAME)
  private String projectIdentifier;
}
