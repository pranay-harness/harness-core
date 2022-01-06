/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import static io.harness.rule.OwnerRule.JASMEET;
import static io.harness.utils.DelegateOwner.getNGTaskSetupAbstractionsWithOwner;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DX)
public class DelegateOwnerTest extends CategoryTest {
  private static final String accountIdentifier = "MOCK_ACCOUNT_IDENTIFIER";
  private static final String orgIdentifier = "MOCK_ORG_IDENTIFIER";
  private static final String projectIdentitifer = "MOCK_PROJECT_IDENTIFIER";

  @Test
  @Owner(developers = JASMEET)
  @Category(UnitTests.class)
  public void shouldCorrectlySetOwner() {
    Map<String, String> ngTaskSetupAbstractionsWithOwner =
        getNGTaskSetupAbstractionsWithOwner(accountIdentifier, null, null);
    assertThat(ngTaskSetupAbstractionsWithOwner.getOrDefault("ng", "")).isEqualTo("true");
    assertThat(ngTaskSetupAbstractionsWithOwner.containsKey("owner")).isEqualTo(false);

    ngTaskSetupAbstractionsWithOwner = getNGTaskSetupAbstractionsWithOwner(accountIdentifier, null, projectIdentitifer);
    assertThat(ngTaskSetupAbstractionsWithOwner.getOrDefault("ng", "")).isEqualTo("true");
    assertThat(ngTaskSetupAbstractionsWithOwner.containsKey("owner")).isEqualTo(false);

    ngTaskSetupAbstractionsWithOwner = getNGTaskSetupAbstractionsWithOwner(accountIdentifier, orgIdentifier, null);
    assertThat(ngTaskSetupAbstractionsWithOwner.getOrDefault("ng", "")).isEqualTo("true");
    assertThat(ngTaskSetupAbstractionsWithOwner.getOrDefault("owner", "")).isEqualTo(orgIdentifier);

    ngTaskSetupAbstractionsWithOwner =
        getNGTaskSetupAbstractionsWithOwner(accountIdentifier, orgIdentifier, projectIdentitifer);
    assertThat(ngTaskSetupAbstractionsWithOwner.getOrDefault("ng", "")).isEqualTo("true");
    assertThat(ngTaskSetupAbstractionsWithOwner.getOrDefault("owner", ""))
        .isEqualTo(orgIdentifier + "/" + projectIdentitifer);
  }
}
