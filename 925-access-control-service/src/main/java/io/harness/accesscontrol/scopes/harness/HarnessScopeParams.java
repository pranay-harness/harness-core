/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.scopes.harness;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

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

  @NotEmpty @QueryParam(ACCOUNT_LEVEL_PARAM_NAME) private String accountIdentifier;
  @QueryParam(ORG_LEVEL_PARAM_NAME) private String orgIdentifier;
  @QueryParam(PROJECT_LEVEL_PARAM_NAME) private String projectIdentifier;
}
