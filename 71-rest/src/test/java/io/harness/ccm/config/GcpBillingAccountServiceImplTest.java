package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.GcpServiceAccountService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GcpBillingAccountServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String billingAccountUuid = "BILLING_ACCOUNT_UUID";
  private String organizationSettingId = "ORGANIZATION_SETTING_ID";
  private String impersonatedServiceAccount = "impersonate-account-1@ccm-play.iam.gserviceaccount.com";
  private GcpOrganization gcpOrganization;
  private String accessToken =
      "ya29.c.KrwCxwfePEABHM3IrQLAqofwSrYad2i0NLiJoZ0Dcb9EY5Ik4igM3sJ4V6DSZS5n2im7XXYBZS2rnScjS5ne-YMHprcRnuhbTuT-LLi3Z9LLHccrbU6RSAFN0TDwbun1zxFEkiiyk84L-TW-a-mAxFYJxs9TtmM6RMrF1mklT-dv4NxrYhA0GBbGlYaBETmVgButyW9iwW3ZDrTx1KI9iT3IWJys7kP2geZ-dfaLfpwpSn3wdOX1GBKEQIoqnmQFvxpvLPerZohuowPdkJz6Hy5_9qScjJbRnBPhQqNPTQp8-9L5jLUXdwJaHMBFo0DWcZRAg6QrIWq0cd6xwKKgvZ8r332bvr5y_ZVecosr7CwaWDmBktJ-Nt41FGLqnYoQYihnHhLPQsJ89i4Dw7qjnDsykm1Bw1B6p6Gf24StwQ";
  @Mock private GcpBillingAccountDao gcpBillingAccountDao;
  @Mock private GcpOrganizationService gcpOrganizationService;
  @Mock private GcpServiceAccountService gcpServiceAccountService;
  @InjectMocks private GcpBillingAccountServiceImpl gcpBillingAccountService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() throws GeneralSecurityException, IOException {
    gcpOrganization = GcpOrganization.builder().serviceAccountEmail(impersonatedServiceAccount).build();
    when(gcpOrganizationService.get(eq(organizationSettingId))).thenReturn(gcpOrganization);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGet() {
    gcpBillingAccountService.get(billingAccountUuid);
    verify(gcpBillingAccountDao).get(eq(billingAccountUuid));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testList() {
    gcpBillingAccountService.list(accountId, organizationSettingId);
    verify(gcpBillingAccountDao).list(eq(accountId), eq(organizationSettingId));
  }
}