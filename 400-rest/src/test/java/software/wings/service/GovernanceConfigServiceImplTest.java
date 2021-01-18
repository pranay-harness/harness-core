package software.wings.service;

import static io.harness.rule.OwnerRule.PRABU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.governance.BlackoutWindowFilterType;
import io.harness.governance.CustomAppFilter;
import io.harness.governance.TimeRangeBasedFreezeConfig;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.service.intfc.compliance.GovernanceConfigService;
import software.wings.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

public class GovernanceConfigServiceImplTest extends WingsBaseTest {
  @Mock FeatureFlagService featureFlagService;
  @Inject @InjectMocks private GovernanceConfigService governanceConfigService;

  @Before
  public void setUp() {
    when(featureFlagService.isEnabled(Matchers.eq(FeatureName.TIME_RANGE_FREEZE_GOVERNANCE_DEV), anyString()))
        .thenReturn(true);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldSaveAndGetDeploymentFreeze() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);

    GovernanceConfig savedGovernanceConfig =
        governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig);
    savedGovernanceConfig.setUuid("GOVERNANCE_CONFIG_ID");
    JsonNode actual = JsonUtils.toJsonNode(savedGovernanceConfig);
    JsonNode expected = JsonUtils.readResourceFile("./governance/governance_config_expected.json", JsonNode.class);
    assertThat(actual).hasToString(expected.toString());
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForCustomEnvWithNonCustomApp() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);
    governanceConfig.getTimeRangeBasedFreezeConfigs().get(0).getAppSelections().get(0).setFilterType(
        BlackoutWindowFilterType.ALL);

    assertThatThrownBy(() -> governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Environment filter type can be CUSTOM only when Application Filter type is CUSTOM");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForCustomEnvWithMultipleApps() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);
    CustomAppFilter appConfig =
        (CustomAppFilter) governanceConfig.getTimeRangeBasedFreezeConfigs().get(0).getAppSelections().get(0);
    appConfig.getApps().add("app2");

    assertThatThrownBy(() -> governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Application filter should have exactly one app when environment filter type is CUSTOM");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForDuplicateNames() {
    GovernanceConfig governanceConfig =
        JsonUtils.readResourceFile("./governance/governance_config.json", GovernanceConfig.class);
    TimeRangeBasedFreezeConfig freeze1 = governanceConfig.getTimeRangeBasedFreezeConfigs().get(0);
    governanceConfig.getTimeRangeBasedFreezeConfigs().add(freeze1);

    assertThatThrownBy(() -> governanceConfigService.upsert(governanceConfig.getAccountId(), governanceConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Duplicate Deployment Freeze name freezer found.");
  }
}
