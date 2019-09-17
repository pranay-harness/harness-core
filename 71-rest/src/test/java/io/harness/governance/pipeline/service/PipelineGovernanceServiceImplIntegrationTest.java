package io.harness.governance.pipeline.service;

import static graphql.Assert.assertNotEmpty;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.governance.pipeline.model.MatchType;
import io.harness.governance.pipeline.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.model.PipelineGovernanceRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtils;
import software.wings.service.intfc.AccountService;

import java.util.Collections;
import java.util.List;

public class PipelineGovernanceServiceImplIntegrationTest extends BaseIntegrationTest {
  @Inject private PipelineGovernanceService pipelineGovernanceService;
  @Inject private WingsPersistence persistence;
  @Inject private AccountService accountService;

  private final String SOME_ACCOUNT_ID =
      randomAlphanumeric(5) + "-some-account-id-" + PipelineGovernanceServiceImplIntegrationTest.class.getSimpleName();

  private boolean indexesEnsured;
  private boolean accountAdded;

  @Before
  public void init() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtils.isManagerRunning(client)) {
      persistence.getDatastore(PipelineGovernanceConfig.class).ensureIndexes(PipelineGovernanceConfig.class);
      indexesEnsured = true;
    }

    if (!accountAdded) {
      long tooFarTime = 1998195261000L;
      LicenseInfo licenseInfo =
          LicenseInfo.builder().accountType(AccountType.PAID).expiryTime(tooFarTime).licenseUnits(50).build();
      Account account = anAccount()
                            .withAccountName("some-account-name")
                            .withCompanyName("some-co-name")
                            .withUuid(SOME_ACCOUNT_ID)
                            .withLicenseInfo(licenseInfo)
                            .build();

      accountService.save(account);
      accountAdded = true;
    }

    persistence.delete(persistence.createQuery(PipelineGovernanceConfig.class));
  }

  @Test
  @Category(IntegrationTests.class)
  public void testList() {
    List<PipelineGovernanceConfig> initialList = pipelineGovernanceService.list(SOME_ACCOUNT_ID);

    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    pipelineGovernanceService.add(SOME_ACCOUNT_ID, config);

    List<PipelineGovernanceConfig> list = pipelineGovernanceService.list(SOME_ACCOUNT_ID);
    assertThat(list.size()).isEqualTo(initialList.size() + 1);
  }

  @Test
  @Category(IntegrationTests.class)
  public void testUpdate() {
    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    PipelineGovernanceConfig addedConfig = pipelineGovernanceService.add(SOME_ACCOUNT_ID, config);

    PipelineGovernanceConfig newConfig =
        new PipelineGovernanceConfig(null, SOME_ACCOUNT_ID, "name-new", "description-new",
            Collections.singletonList(new PipelineGovernanceRule(Collections.emptyList(), MatchType.ALL, 10, "")),
            Collections.emptyList());
    PipelineGovernanceConfig newlyAdded =
        pipelineGovernanceService.update(SOME_ACCOUNT_ID, addedConfig.getUuid(), newConfig);

    assertThat("name-new").isEqualTo(newlyAdded.getName());
    assertThat("description-new").isEqualTo(newlyAdded.getDescription());
    assertNotEmpty(newlyAdded.getRules());
    assertThat(newlyAdded.getRules().get(0).getWeight()).isEqualTo(10);
    assertThat(newlyAdded.getRestrictions().get(0).getAppIds().get(0)).isEqualTo("test-app-id");
  }

  @Test
  @Category(IntegrationTests.class)
  public void testAdd() {
    PipelineGovernanceConfig config = new PipelineGovernanceConfig(
        null, SOME_ACCOUNT_ID, "name", "description", Collections.emptyList(), Collections.emptyList());

    PipelineGovernanceConfig addedConfig = pipelineGovernanceService.add(SOME_ACCOUNT_ID, config);
    assertThat(addedConfig.getUuid()).isNotNull();
  }
}
