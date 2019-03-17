package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.compliance.GovernanceConfigService;

/**
 *
 * @author rktummala
 */
public class GovernanceConfigServiceTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private Account account;

  @InjectMocks @Inject private GovernanceConfigService governanceConfigService;

  private String accountId = UUIDGenerator.generateUuid();

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(accountService.get(anyString())).thenReturn(account);
  }

  /**
   * Should save and read.
   *
   */
  @Test
  @Category(UnitTests.class)
  public void testUpdateAndRead() {
    GovernanceConfig defaultConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
    GovernanceConfig governanceConfig = governanceConfigService.get(accountId);
    compare(defaultConfig, governanceConfig);

    GovernanceConfig inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(true).build();
    GovernanceConfig savedGovernanceConfig = governanceConfigService.update(accountId, inputConfig);
    compare(inputConfig, savedGovernanceConfig);

    savedGovernanceConfig = governanceConfigService.get(accountId);
    compare(inputConfig, savedGovernanceConfig);

    inputConfig = GovernanceConfig.builder().accountId(accountId).deploymentFreeze(false).build();
    savedGovernanceConfig = governanceConfigService.update(accountId, inputConfig);
    compare(inputConfig, savedGovernanceConfig);

    savedGovernanceConfig = governanceConfigService.get(accountId);
    compare(inputConfig, savedGovernanceConfig);
  }

  private void compare(GovernanceConfig lhs, GovernanceConfig rhs) {
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.isDeploymentFreeze(), rhs.isDeploymentFreeze());
  }
}
