package software.wings.verification;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.serializer.YamlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.sm.StateType;
import software.wings.sm.states.DatadogState.Metric;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration.DatadogCVConfigurationYaml;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DatadogCVConfigurationYamlHandlerTest extends CVConfigurationYamlHandlerTestBase {
  DatadogCvConfigurationYamlHandler yamlHandler = new DatadogCvConfigurationYamlHandler();

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    setupTests(yamlHandler);
  }

  private void setBasicInfo(DatadogCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.NEW_RELIC);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setName("TestDDConfig");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToYaml() {
    final String appId = "appId";
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVServiceConfiguration cvServiceConfiguration =
        DatadogCVServiceConfiguration.builder().dockerMetrics(dockerMetrics).build();
    setBasicInfo(cvServiceConfiguration);

    DatadogCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertThat(yaml.getServiceName()).isEqualTo(serviceName);
    assertThat(yaml.getDockerMetrics().keySet().iterator().next())
        .isEqualTo(cvServiceConfiguration.getDockerMetrics().keySet().iterator().next());
    assertThat(yaml.getDockerMetrics().values()).isEqualTo(cvServiceConfiguration.getDockerMetrics().values());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDDConfig.yaml")).thenReturn("TestDDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestDDConfig.yaml").build();
    changeContext.setChange(c);
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().dockerMetrics(dockerMetrics).build();
    buildYaml(yaml);
    changeContext.setYaml(yaml);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestDDConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertDDCustomMetric() throws Exception {
    URL yamlPath = getClass().getClassLoader().getResource("./verification/datadogCVConfigCustomYaml.yaml");
    String yamlString = Resources.toString(yamlPath, Charsets.UTF_8);
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDDConfig.yaml")).thenReturn("TestDDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestDDConfig.yaml").build();
    changeContext.setChange(c);
    YamlUtils yamlUtils = new YamlUtils();
    DatadogCVConfigurationYaml yaml = yamlUtils.read(yamlString, new TypeReference<DatadogCVConfigurationYaml>() {});
    buildYaml(yaml);
    changeContext.setYaml(yaml);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestDDConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
    assertThat(bean.getCustomMetrics().size()).isEqualTo(1);
    assertThat(bean.getCustomMetrics().values().size()).isEqualTo(1);
    Set<Metric> metrics = bean.getCustomMetrics().values().iterator().next();
    assertThat(metrics.iterator().next().getMetricName()).isIn(Arrays.asList("testMetric.test2", "testMetric.test"));
  }

  @Test(expected = WingsException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertMissingMetrics() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().dockerMetrics(null).build();
    buildYaml(yaml);
    changeContext.setYaml(yaml);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertIncorrectMetrics() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);

    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage123");

    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().dockerMetrics(dockerMetrics).build();
    buildYaml(yaml);

    changeContext.setYaml(yaml);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertMissingAppFilterHasServiceName() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().dockerMetrics(dockerMetrics).build();
    buildYaml(yaml);
    changeContext.setYaml(yaml);
    dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");
    yaml.setDockerMetrics(dockerMetrics);
    yaml.setDatadogServiceName("todolist");
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getDatadogServiceName()).isEqualTo("todolist");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertAlreadyExisting() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestDDConfig.yaml")).thenReturn("TestDDConfig");

    DatadogCVServiceConfiguration cvConfig = DatadogCVServiceConfiguration.builder().build();
    cvConfig.setUuid("testUUID");
    when(cvConfigurationService.getConfiguration("TestDDConfig", appId, envId)).thenReturn(cvConfig);
    ChangeContext<DatadogCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId(accountId).withFilePath("TestDDConfig.yaml").build();
    changeContext.setChange(c);
    Map<String, String> dockerMetrics = new HashMap<>();
    dockerMetrics.put("cluster:harness-test", "kubernetes.cpu.usage.total");

    DatadogCVConfigurationYaml yaml = DatadogCVConfigurationYaml.builder().dockerMetrics(dockerMetrics).build();
    buildYaml(yaml);
    changeContext.setYaml(yaml);
    DatadogCVServiceConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestDDConfig");
    assertThat(bean.getUuid()).isEqualTo(cvConfig.getUuid());
  }
}
