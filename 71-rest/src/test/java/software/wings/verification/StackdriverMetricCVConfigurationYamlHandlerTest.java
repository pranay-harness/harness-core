package software.wings.verification;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.verification.CVConfigurationServiceImplTest;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration.StackDriverMetricCVConfigurationYaml;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import java.util.Arrays;
import java.util.List;

public class StackdriverMetricCVConfigurationYamlHandlerTest extends CVConfigurationYamlHandlerTestBase {
  @Inject StackDriverMetricsCVConfigurationYamlHandler yamlHandler;

  @Before
  public void setup() throws Exception {
    setupTests(yamlHandler);
  }

  private List<StackDriverMetricDefinition> getMetricDefinitions() throws Exception {
    String paramsForStackDriver = Resources.toString(
        CVConfigurationServiceImplTest.class.getResource("/apm/stackdriverpayload.json"), Charsets.UTF_8);
    StackDriverMetricDefinition definition = StackDriverMetricDefinition.builder()
                                                 .filterJson(paramsForStackDriver)
                                                 .metricName("metricName")
                                                 .metricType("INFRA")
                                                 .txnName("Group")
                                                 .build();

    return Arrays.asList(definition);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testToYaml() throws Exception {
    final String appId = "appId";
    StackDriverMetricCVConfiguration cvServiceConfiguration =
        StackDriverMetricCVConfiguration.builder().metricDefinitions(getMetricDefinitions()).build();
    setBasicInfo(cvServiceConfiguration);

    StackDriverMetricCVConfigurationYaml yaml = yamlHandler.toYaml(cvServiceConfiguration, appId);

    assertThat(yaml.getName()).isEqualTo(cvServiceConfiguration.getName());
    assertThat(yaml.getAccountId()).isEqualTo(cvServiceConfiguration.getAccountId());
    assertThat(yaml.getServiceName()).isEqualTo(serviceName);
    assertThat(yaml.getEnvName()).isEqualTo(envName);
    assertThat(yaml.getMetricDefinitions()).isEqualTo(getMetricDefinitions());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsert() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestStackdriverConfig.yaml")).thenReturn("TestStackdriverConfig");

    ChangeContext<StackDriverMetricCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c =
        Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestStackdriverConfig.yaml").build();
    changeContext.setChange(c);
    StackDriverMetricCVConfigurationYaml yaml =
        StackDriverMetricCVConfigurationYaml.builder().metricDefinitions(getMetricDefinitions()).build();
    buildYaml(yaml);
    changeContext.setYaml(yaml);
    StackDriverMetricCVConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);

    assertThat(bean.getName()).isEqualTo("TestStackdriverConfig");
    assertThat(bean.getAppId()).isEqualTo(appId);
    assertThat(bean.getEnvId()).isEqualTo(envId);
    assertThat(bean.getServiceId()).isEqualTo(serviceId);
    assertThat(bean.getUuid()).isNotNull();
  }

  @Test(expected = VerificationOperationException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsertBadMetridDefinition() throws Exception {
    when(yamlHelper.getAppId(anyString(), anyString())).thenReturn(appId);
    when(yamlHelper.getEnvironmentId(anyString(), anyString())).thenReturn(envId);
    when(yamlHelper.getNameFromYamlFilePath("TestAppDConfig.yaml")).thenReturn("TestAppDConfig");

    ChangeContext<StackDriverMetricCVConfigurationYaml> changeContext = new ChangeContext<>();
    Change c = Change.Builder.aFileChange().withAccountId("accountId").withFilePath("TestAppDConfig.yaml").build();
    changeContext.setChange(c);
    StackDriverMetricCVConfigurationYaml yaml =
        StackDriverMetricCVConfigurationYaml.builder().metricDefinitions(getMetricDefinitions()).build();
    buildYaml(yaml);
    yaml.setMetricDefinitions(Arrays.asList(StackDriverMetricDefinition.builder().build()));
    changeContext.setYaml(yaml);
    StackDriverMetricCVConfiguration bean = yamlHandler.upsertFromYaml(changeContext, null);
  }
}
