package software.wings.features;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.service.impl.PreDeploymentCheckerTest.getWorkflow;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse.PageResponseBuilder;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.features.api.PremiumFeature;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowService;

import java.util.Collection;
import java.util.Collections;

public class FlowControlFeatureTest extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "ACCOUNT_ID";

  @Mock private WorkflowService workflowService;
  @InjectMocks @Inject @Named(FlowControlFeature.FEATURE_NAME) private PremiumFeature flowControlFeature;
  @Inject AccountService accountService;

  @Before
  public void setUp() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.COMMUNITY);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setLicenseUnits(100);
    licenseInfo.setExpiryTime(System.nanoTime());

    accountService.save(anAccount()
                            .withUuid(TEST_ACCOUNT_ID)
                            .withCompanyName("Harness")
                            .withAccountName("Harness")
                            .withAccountKey("ACCOUNT_KEY")
                            .withLicenseInfo(licenseInfo)
                            .build());
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void workflowWithFlowControl() {
    when(workflowService.listWorkflows(Mockito.any(PageRequest.class)))
        .thenReturn(
            PageResponseBuilder.aPageResponse().withResponse(Collections.singletonList(getWorkflow(true))).build());

    Collection<Usage> disallowedUsages = flowControlFeature.getDisallowedUsages(TEST_ACCOUNT_ID, AccountType.COMMUNITY);

    assertThat(disallowedUsages).isNotNull();
    assertThat(disallowedUsages).hasSize(1);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void workflowWithoutFlowControl() {
    when(workflowService.listWorkflows(Mockito.any(PageRequest.class)))
        .thenReturn(
            PageResponseBuilder.aPageResponse().withResponse(Collections.singletonList(getWorkflow(false))).build());

    Collection<Usage> disallowedUsages = flowControlFeature.getDisallowedUsages(TEST_ACCOUNT_ID, AccountType.COMMUNITY);

    assertThat(disallowedUsages).isNotNull();
    assertThat(disallowedUsages).isEmpty();
  }
}
