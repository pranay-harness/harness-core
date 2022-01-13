/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AccountDaoTest extends WingsBaseTest {
  private CeLicenseInfo ceLicenseInfo;
  @Inject private AccountDao accountDao;

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpdateCeLicense() {
    ceLicenseInfo = CeLicenseInfo.builder().build();
    String accountId = accountDao.save(Account.Builder.anAccount().build());
    accountDao.updateCeLicense(accountId, ceLicenseInfo);
    Account updatedAccount = accountDao.get(accountId);
    assertThat(updatedAccount).extracting("ceLicenseInfo").isEqualTo(ceLicenseInfo);
  }
}
