/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.security.restrictions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.EntityReference;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 06/24/18
 */
@OwnedBy(PL)
@Data
@Builder
public class AppRestrictionsSummary {
  private EntityReference application;
  private boolean hasAllProdEnvAccess;
  private boolean hasAllNonProdEnvAccess;
  private Set<EntityReference> environments;
}
