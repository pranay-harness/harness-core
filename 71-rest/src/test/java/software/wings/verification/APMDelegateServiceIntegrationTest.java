package software.wings.verification;

import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;
import io.harness.scm.SecretName;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.DatadogConfig;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.APMDelegateService;
import software.wings.service.impl.analysis.APMDelegateServiceImpl;
import software.wings.sm.states.APMVerificationState.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class APMDelegateServiceIntegrationTest extends BaseIntegrationTest {
  APMDelegateService apmDelegateService;
  String apiKey;
  String appKey;
  Map<String, String> headers = new HashMap<>();
  Map<String, String> options = new HashMap<>();

  @Before
  public void setupKeys() {
    apmDelegateService = new APMDelegateServiceImpl();
    apiKey = scmSecret.decryptToString(new SecretName("datadog_api_key"));
    appKey = scmSecret.decryptToString(new SecretName("datadog_application_key"));

    options.put("application_key", appKey);
    options.put("api_key", apiKey);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  public void testValidateConnectorHappyCase() {
    options.put("from", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    options.put("to", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    APMValidateCollectorConfig config = APMValidateCollectorConfig.builder()
                                            .collectionMethod(Method.GET)
                                            .baseUrl("https://app.datadoghq.com/api/v1/")
                                            .options(options)
                                            .headers(headers)
                                            .url("metrics")
                                            .build();
    boolean validate = apmDelegateService.validateCollector(config);

    assertThat(validate).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  public void testValidateConnectorHappyCaseNoCollectionMethod() {
    options.put("from", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    options.put("to", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    APMValidateCollectorConfig config = APMValidateCollectorConfig.builder()
                                            .baseUrl("https://app.datadoghq.com/api/v1/")
                                            .options(options)
                                            .headers(headers)
                                            .url("metrics")
                                            .build();
    boolean validate = apmDelegateService.validateCollector(config);

    assertThat(validate).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  public void testValidateConnectorHappyCaseDatadogConfig() {
    DatadogConfig ddConfig =
        DatadogConfig.builder()
            .url("https://app.datadoghq.com/api/v1/")
            .apiKey(scmSecret.decryptToCharArray(new SecretName("datadog_api_key")))
            .applicationKey(scmSecret.decryptToCharArray(new SecretName("datadog_application_key")))
            .build();

    options.put("from", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    options.put("to", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    APMValidateCollectorConfig config = ddConfig.createAPMValidateCollectorConfig();
    boolean validate = apmDelegateService.validateCollector(config);

    assertThat(validate).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  public void testValidateConnectorHappyCasePost() {
    APMValidateCollectorConfig config =
        APMValidateCollectorConfig.builder()
            .collectionMethod(Method.POST)
            .body(
                "{\"query\": \"env:production status:error service:app-thunder-storefront pod_name:pod_abc\",\"time\": {\"from\": \"now - 1h\", \"to\": \"now\"}, \"sort\": \"desc\", \"limit\": 2, \"index\": \"trace-search\"}")
            .baseUrl("https://app.datadoghq.com/api/v1/")
            .options(options)
            .headers(headers)
            .url("logs-queries/list?")
            .build();
    boolean validate = apmDelegateService.validateCollector(config);

    assertThat(validate).isTrue();
  }
}
