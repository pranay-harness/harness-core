/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.rbac;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.PrincipalType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PrincipalTypeProtoToPrincipalTypeMapperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testConvertToAccessControlPrincipalType() {
    PrincipalType[] principalTypes = PrincipalType.values();
    for (PrincipalType principalType : principalTypes) {
      if (principalType == PrincipalType.UNRECOGNIZED || principalType == PrincipalType.UNKNOWN) {
        continue;
      }
      io.harness.accesscontrol.principals.PrincipalType principalTypeDto =
          PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(principalType);
      assertThat(principalTypeDto).isNotNull();
    }
  }
}
