/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

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
      PrincipalTypeProtoToPrincipalTypeMapper.convertToAccessControlPrincipalType(principalType);
    }
  }
}
