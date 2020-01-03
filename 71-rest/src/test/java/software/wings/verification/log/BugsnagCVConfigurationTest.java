package software.wings.verification.log;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.sm.StateType;

@Slf4j
public class BugsnagCVConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.BUG_SNAG;

  private static final String orgId = "orgId";
  private static final String projectId = "projectId";
  private static final String releaseStage = "releaseStage";
  private static final String query = "query";

  private BugsnagCVConfiguration createBugSnagConfig() {
    BugsnagCVConfiguration config = new BugsnagCVConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setOrgId(orgId);
    config.setProjectId(projectId);
    config.setReleaseStage(releaseStage);
    config.setBrowserApplication(true);
    config.setQuery(query);

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneBugSnagConfig() {
    BugsnagCVConfiguration config = createBugSnagConfig();

    BugsnagCVConfiguration clonedConfig = (BugsnagCVConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(clonedConfig.getOrgId()).isEqualTo(orgId);
    assertThat(clonedConfig.getProjectId()).isEqualTo(projectId);
    assertThat(clonedConfig.getReleaseStage()).isEqualTo(releaseStage);
    assertThat(clonedConfig.browserApplication).isTrue();
    assertThat(clonedConfig.getQuery()).isEqualTo(query);
  }
}