package software.wings.verification.log;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.sm.StateType;

@Slf4j
public class ElkCVConfigurationTest extends WingsBaseTest {
  private static final String configName = "configName";
  private static final String accountId = "accountId";
  private static final String connectorId = "connectorId";
  private static final String envId = "envId";
  private static final String serviceId = "serviceId";
  private static final StateType stateType = StateType.ELK;

  private static final ElkQueryType queryType = ElkQueryType.MATCH;
  private static final String index = "index";
  private static final String hostnameField = "hostnameField";
  private static final String messageField = "messageField";
  private static final String query = "query";
  private static final String timestampField = "timestampField";
  private static final String timestampFormat = "timestampFormat";

  private ElkCVConfiguration createElkConfig() {
    ElkCVConfiguration config = new ElkCVConfiguration();
    config.setName(configName);
    config.setAccountId(accountId);
    config.setConnectorId(connectorId);
    config.setEnvId(envId);
    config.setServiceId(serviceId);
    config.setStateType(stateType);
    config.setEnabled24x7(true);

    config.setQueryType(queryType);
    config.setIndex(index);
    config.setHostnameField(hostnameField);
    config.setMessageField(messageField);
    config.setQuery(query);
    config.setTimestampFormat(timestampFormat);
    config.setTimestampField(timestampField);

    return config;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCloneElkConfig() {
    ElkCVConfiguration config = createElkConfig();

    ElkCVConfiguration clonedConfig = (ElkCVConfiguration) config.deepCopy();

    assertThat(clonedConfig.getName()).isEqualTo(configName);
    assertThat(clonedConfig.getAccountId()).isEqualTo(accountId);
    assertThat(clonedConfig.getConnectorId()).isEqualTo(connectorId);
    assertThat(clonedConfig.getEnvId()).isEqualTo(envId);
    assertThat(clonedConfig.getServiceId()).isEqualTo(serviceId);
    assertThat(clonedConfig.getStateType()).isEqualTo(stateType);
    assertThat(clonedConfig.isEnabled24x7()).isTrue();

    assertThat(clonedConfig.getQueryType()).isEqualTo(queryType);
    assertThat(clonedConfig.getIndex()).isEqualTo(index);
    assertThat(clonedConfig.getHostnameField()).isEqualTo(hostnameField);
    assertThat(clonedConfig.getMessageField()).isEqualTo(messageField);
    assertThat(clonedConfig.getQuery()).isEqualTo(query);
    assertThat(clonedConfig.getTimestampField()).isEqualTo(timestampField);
    assertThat(clonedConfig.getTimestampFormat()).isEqualTo(timestampFormat);
  }
}