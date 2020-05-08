package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.iam.v1.model.ServiceAccount;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.GcpServiceAccountService;
import io.harness.rule.Owner;
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
import software.wings.service.intfc.SettingsService;

public class GcpOrganizationServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String organizationName1 = "ORGANIZATION_NAME_1";
  private String organizationName2 = "ORGANIZATION_NAME_2";
  private String serviceAccountEmail1 = "SERVICE_ACCOUNT_EMAIL_1";
  private GcpOrganization gcpOrganization1;
  private GcpOrganization gcpOrganization2;
  private String serviceAccountEmail = "SERVICE_ACCOUNT_EMAIL";

  private ServiceAccount serviceAccount = new ServiceAccount();

  @Captor ArgumentCaptor<GcpOrganization> gcpOrganizationCaptor;
  @Mock private GcpOrganizationDao gcpOrganizationDao;
  @Mock private GcpServiceAccountService gcpServiceAccountService;
  @Mock private SettingsService settingsService;
  @InjectMocks private GcpOrganizationServiceImpl gcpOrganizationService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    gcpOrganization1 = GcpOrganization.builder()
                           .accountId(accountId)
                           .organizationName(organizationName1)
                           .serviceAccountEmail(serviceAccountEmail)
                           .build();
    gcpOrganization2 = GcpOrganization.builder().accountId(accountId).organizationName(organizationName2).build();
    serviceAccount.setEmail(serviceAccountEmail);
    when(gcpServiceAccountService.create(anyString(), anyString())).thenReturn(serviceAccount);
    when(gcpOrganizationDao.upsert(any(GcpOrganization.class))).thenReturn(GcpOrganization.builder().build());
    when(gcpOrganizationDao.save(any(GcpOrganization.class))).thenReturn("GCP_ORGANIZATION_UUID");
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testCreate() {
    gcpOrganizationService.upsert(gcpOrganization1);
    verify(gcpOrganizationDao).upsert(gcpOrganizationCaptor.capture());
    assertThat(gcpOrganizationCaptor.getValue().getServiceAccountEmail()).isEqualTo(serviceAccountEmail);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testGet() {
    gcpOrganizationService.upsert(gcpOrganization1);
    verify(gcpOrganizationDao).upsert(eq(gcpOrganization1));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldList() {
    gcpOrganizationService.list(accountId);
    verify(gcpOrganizationDao).list(accountId);
  }
}