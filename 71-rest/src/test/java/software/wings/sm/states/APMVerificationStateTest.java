package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDER;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;
import static software.wings.sm.states.APMVerificationState.URL_BODY_APPENDER;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.STATE_EXECUTION_ID;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Injector;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import io.harness.serializer.YamlUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.APMVerificationState.Method;
import software.wings.sm.states.APMVerificationState.MetricCollectionInfo;
import software.wings.sm.states.APMVerificationState.ResponseMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class APMVerificationStateTest extends WingsBaseTest {
  @Inject private Injector injector;
  @Mock private WorkflowStandardParams workflowStandardParameters;

  private ExecutionContextImpl context;

  /**
   * Sets context.
   */
  @Before
  public void setupContext() {
    StateExecutionInstance stateExecutionInstance =
        aStateExecutionInstance().displayName("healthCheck1").uuid(STATE_EXECUTION_ID).build();
    when(workflowStandardParameters.getApp()).thenReturn(anApplication().uuid(APP_ID).name(APP_NAME).build());
    when(workflowStandardParameters.getEnv())
        .thenReturn(anEnvironment().uuid(ENV_ID).name(ENV_NAME).environmentType(EnvironmentType.NON_PROD).build());
    when(workflowStandardParameters.getAppId()).thenReturn(APP_ID);
    when(workflowStandardParameters.getEnvId()).thenReturn(ENV_ID);

    when(workflowStandardParameters.getElementType()).thenReturn(ContextElementType.STANDARD);
    context = new ExecutionContextImpl(stateExecutionInstance, null, injector);
    context.pushContextElement(workflowStandardParameters);
    context.pushContextElement(aHostElement().hostName("localhost").build());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void metricCollectionInfos() throws IOException {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);

    Map<String, List<APMMetricInfo>> apmMetricInfos =
        APMVerificationState.buildMetricInfoMap(apmVerificationState.getMetricCollectionInfos(), Optional.of(context));
    assertThat(3).isEqualTo(apmMetricInfos.size());
    assertThat(2).isEqualTo(apmMetricInfos.get("query").size());
    assertThat(apmMetricInfos.get("query").get(0).getResponseMappers().get("txnName").getFieldValue()).isNotNull();
    assertThat(apmMetricInfos.get("query").get(1).getResponseMappers().get("txnName").getJsonPath()).isNotNull();

    String body = "this is a dummy collection body";
    assertThat(apmMetricInfos.get("queryWithHost" + URL_BODY_APPENDER + body)).hasSize(1);
    assertThat(apmMetricInfos.get("queryWithHost" + URL_BODY_APPENDER + body).get(0).getBody()).isEqualTo(body);
    assertThat(apmMetricInfos.get("queryWithHost" + URL_BODY_APPENDER + body).get(0).getMethod())
        .isEqualTo(Method.POST);

    assertThat(apmMetricInfos.get("queryWithHost")).hasSize(1);
    APMMetricInfo metricWithHost = apmMetricInfos.get("queryWithHost").get(0);
    assertThat(metricWithHost.getResponseMappers().get("host").getJsonPath()).isNotNull();
    assertThat(metricWithHost.getResponseMappers().get("host").getRegexs()).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateFields() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    apmVerificationState.setMetricCollectionInfos(null);
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("Metric Collection Info");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateFieldsResponseMapping() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info =
        MetricCollectionInfo.builder().collectionUrl("This is a sample URL").metricName("name").build();
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("responseMapping");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateFieldsResponseMappingMetricValue() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info =
        MetricCollectionInfo.builder().collectionUrl("This is a sample URL").metricName("name").build();
    ResponseMapping mapping =
        ResponseMapping.builder().metricValueJsonPath("metricValue").timestampJsonPath("timestamp").build();
    info.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(invalidFields.size() == 1).isTrue();
    assertThat(invalidFields.keySet().iterator().next()).isEqualTo("transactionName");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateFieldsResponseMappingHostName() {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info =
        MetricCollectionInfo.builder().collectionUrl("This is a sample URL ${host}").metricName("name").build();
    ResponseMapping mapping = ResponseMapping.builder()
                                  .metricValueJsonPath("metricValue")
                                  .timestampJsonPath("timestamp")
                                  .txnNameFieldValue("txnName")
                                  .build();
    info.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(invalidFields.size() == 0).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHostAndBaseline() {
    String metricName = generateUuid();
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info = MetricCollectionInfo.builder()
                                    .collectionUrl("This is a sample URL " + VERIFICATION_HOST_PLACEHOLDER)
                                    .baselineCollectionUrl("some baseline url")
                                    .metricName(metricName)
                                    .build();
    ResponseMapping mapping = ResponseMapping.builder()
                                  .metricValueJsonPath("metricValue")
                                  .timestampJsonPath("timestamp")
                                  .txnNameFieldValue("txnName")
                                  .build();
    info.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(invalidFields).hasSize(1);
    assertThat("for " + metricName + " the collection url has " + VERIFICATION_HOST_PLACEHOLDER
        + " and baseline collection url as well")
        .isEqualTo(invalidFields.get("collectionUrl"));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testBaselineUrlHasHost() {
    String metricName = generateUuid();
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info = MetricCollectionInfo.builder()
                                    .collectionUrl("This is a sample URL")
                                    .baselineCollectionUrl("some baseline url " + VERIFICATION_HOST_PLACEHOLDER)
                                    .metricName(metricName)
                                    .build();
    ResponseMapping mapping = ResponseMapping.builder()
                                  .metricValueJsonPath("metricValue")
                                  .timestampJsonPath("timestamp")
                                  .txnNameFieldValue("txnName")
                                  .build();
    info.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(invalidFields).hasSize(1);
    assertThat("Baseline url can only be set for canary verification strategy. For " + metricName
        + " there is baseline url set " + VERIFICATION_HOST_PLACEHOLDER)
        .isEqualTo(invalidFields.get("collectionUrl"));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testInvalidMultipleBaselineUrl() {
    String metricName1 = generateUuid();
    String metricName2 = generateUuid();
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    MetricCollectionInfo info1 = MetricCollectionInfo.builder()
                                     .collectionUrl("This is a sample URL")
                                     .baselineCollectionUrl("some baseline url")
                                     .metricName(metricName1)
                                     .build();
    MetricCollectionInfo info2 = MetricCollectionInfo.builder()
                                     .collectionUrl("This is a sample URL " + VERIFICATION_HOST_PLACEHOLDER)
                                     .metricName(metricName2)
                                     .build();
    ResponseMapping mapping = ResponseMapping.builder()
                                  .metricValueJsonPath("metricValue")
                                  .timestampJsonPath("timestamp")
                                  .txnNameFieldValue("txnName")
                                  .build();
    info1.setResponseMapping(mapping);
    info2.setResponseMapping(mapping);
    apmVerificationState.setMetricCollectionInfos(Arrays.asList(info1, info2));
    Map<String, String> invalidFields = apmVerificationState.validateFields();
    assertThat(1).isEqualTo(invalidFields.size());
    assertThat("for " + metricName2 + " the url has " + VERIFICATION_HOST_PLACEHOLDER
        + ". When configuring multi url verification all metrics should follow the same pattern.")
        .isEqualTo(invalidFields.get("collectionUrl"));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidInitialDelay() throws Exception {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);
    apmVerificationState.setInitialAnalysisDelay("4m");
    assertThat(apmVerificationState.validateFields().containsKey("initialAnalysisDelay")).isFalse();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testInValidInitialDelay_Minutes() throws Exception {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);
    apmVerificationState.setInitialAnalysisDelay("40m");
    assertThat(apmVerificationState.validateFields().containsKey("initialAnalysisDelay")).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidInitialDelay_Seconds() throws Exception {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);
    apmVerificationState.setInitialAnalysisDelay("200s");
    assertThat(apmVerificationState.validateFields().containsKey("initialAnalysisDelay")).isFalse();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testInValidInitialDelay_Seconds() throws Exception {
    APMVerificationState apmVerificationState = new APMVerificationState("dummy");
    YamlUtils yamlUtils = new YamlUtils();
    String yamlStr =
        Resources.toString(APMVerificationStateTest.class.getResource("/apm/apm_config.yml"), Charsets.UTF_8);
    List<APMVerificationState.MetricCollectionInfo> mcInfo =
        yamlUtils.read(yamlStr, new TypeReference<List<APMVerificationState.MetricCollectionInfo>>() {});
    apmVerificationState.setMetricCollectionInfos(mcInfo);
    apmVerificationState.setInitialAnalysisDelay("500s");
    assertThat(apmVerificationState.validateFields().containsKey("initialAnalysisDelay")).isTrue();
  }
}
