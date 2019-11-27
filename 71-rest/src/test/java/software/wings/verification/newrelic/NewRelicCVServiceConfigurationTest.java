package software.wings.verification.newrelic;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.List;

@Slf4j
public class NewRelicCVServiceConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.NEW_RELIC;

  private static final String applicationId = "applicationId";

  private List<String> getMetrics() {
    return Lists.newArrayList("metric1", "metric2");
  }

  private NewRelicCVServiceConfiguration createNewRelicConfig() {
    NewRelicCVServiceConfiguration config = new NewRelicCVServiceConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setApplicationId(applicationId);
    config.setMetrics(getMetrics());

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneNewRelicConfig() {
    NewRelicCVServiceConfiguration config = createNewRelicConfig();

    NewRelicCVServiceConfiguration clonedConfig = (NewRelicCVServiceConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(clonedConfig.getApplicationId()).isEqualTo(applicationId);
    assertThat(new HashSet<>(clonedConfig.getMetrics())).isEqualTo(new HashSet<>(getMetrics()));
  }
}