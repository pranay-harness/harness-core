/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
public class LdapLinkGroupRequest {
  @NotBlank String ldapGroupDN;
  @NotBlank String ldapGroupName;
}
