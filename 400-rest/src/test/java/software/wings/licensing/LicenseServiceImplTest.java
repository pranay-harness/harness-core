/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.licensing;

import static io.harness.ccm.license.CeLicenseType.Constants.CE_TRIAL_PERIOD_DAYS;
import static io.harness.ccm.license.CeLicenseType.LIMITED_TRIAL;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.license.CeLicenseType;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AccountDao;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class LicenseServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private Account account = Account.Builder.anAccount().withUuid(accountId).build();
  private CeLicenseInfo licenseInfo = CeLicenseInfo.builder().licenseType(LIMITED_TRIAL).build();
  @Captor ArgumentCaptor<CeLicenseInfo> ceLicenseInfoArgumentCaptor;

  @Mock private AccountService accountService;
  @Mock private AccountDao accountDao;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private GenericDbCache dbCache;
  @Mock private ExecutorService executorService;
  @Mock private LicenseProvider licenseProvider;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private EventPublishHelper eventPublishHelper;
  @Mock private MainConfiguration mainConfiguration;
  @InjectMocks private LicenseServiceImpl licenseService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    when(accountDao.get(eq(accountId))).thenReturn(account);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldStartCeLimitedTrial() {
    licenseService.startCeLimitedTrial(accountId);
    verify(accountDao).updateCeLicense(eq(accountId), ceLicenseInfoArgumentCaptor.capture());
    CeLicenseInfo updatedCeLicenseInfo = ceLicenseInfoArgumentCaptor.getValue();
    assertThat(updatedCeLicenseInfo.getLicenseType()).isEqualTo(LIMITED_TRIAL);

    long expiryTime = Math.max(CeLicenseType.getEndOfYearAsMillis(2020),
        LocalDate.now().plusDays(CE_TRIAL_PERIOD_DAYS).toDate().toInstant().toEpochMilli());
    assertThat(updatedCeLicenseInfo.getExpiryTime())
        .isCloseTo(expiryTime, offset(Duration.of(1, ChronoUnit.DAYS).toMillis()));
  }
}
