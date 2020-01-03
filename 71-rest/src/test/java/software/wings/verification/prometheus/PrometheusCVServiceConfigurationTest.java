package software.wings.verification.prometheus;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.sm.StateType;

import java.util.HashSet;
import java.util.List;

@Slf4j
public class PrometheusCVServiceConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.DYNA_TRACE;

  private List<TimeSeries> getTimeSeries() {
    return Lists.newArrayList(TimeSeries.builder().metricName("metric1").metricType("type1").build(),
        TimeSeries.builder().metricName("metric2").metricType("type2").build());
  }

  private PrometheusCVServiceConfiguration createPrometheusConfig() {
    PrometheusCVServiceConfiguration config = new PrometheusCVServiceConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setTimeSeriesToAnalyze(getTimeSeries());

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneDynaTraceConfig() {
    PrometheusCVServiceConfiguration config = createPrometheusConfig();

    PrometheusCVServiceConfiguration clonedConfig = (PrometheusCVServiceConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(new HashSet<>(clonedConfig.getTimeSeriesToAnalyze())).isEqualTo(new HashSet<>(getTimeSeries()));
  }
}