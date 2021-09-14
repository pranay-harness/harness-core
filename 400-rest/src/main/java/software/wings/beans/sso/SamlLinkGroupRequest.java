/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Swapnil
 */
@OwnedBy(PL)
@Data
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SamlLinkGroupRequest {
  @NotBlank String samlGroupName;
}
