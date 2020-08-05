package io.harness.cvng.core.dsl;

import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.SimulationSource;
import io.specto.hoverfly.junit.core.SslConfigurer;
import io.specto.hoverfly.junit.rule.HoverflyRule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Sample test to use Hoverfly with OkHttpClient.
 */
public class HoverflyExampleTest extends CategoryTest {
  // TODO: This is just basic example for Hoverfly. We need make capture and simulation switching easier.
  @ClassRule
  public static final HoverflyRule rule =
      HoverflyRule.inSimulationMode(HoverflyConfig.localConfigs().disableTlsVerification());
  // In capture mode-
  // public static final HoverflyRule rule =
  // HoverflyRule.inCaptureMode(HoverflyConfig.localConfigs().disableTlsVerification());

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testQAAPI() throws IOException {
    rule.simulate(SimulationSource.file(Paths.get("src/test/resources/hoverfly/qa-api-version-test-file.json")));
    // This needs to be done to run tests faster. uncomment following line to capture the actual request.
    // rule.capture("qa-api-version-test-file.json");

    SslConfigurer sslConfigurer = rule.getSslConfigurer();

    OkHttpClient client =
        new OkHttpClient()
            .newBuilder()
            .sslSocketFactory(sslConfigurer.getSslContext().getSocketFactory(), sslConfigurer.getTrustManager())
            .build();
    Request request = new Request.Builder().url("https://qa.harness.io/api/version").method("GET", null).build();
    Response response = client.newCall(request).execute();
    assertThat(response.code()).isEqualTo(200);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProdAPI() throws IOException {
    rule.simulate(SimulationSource.file(Paths.get("src/test/resources/hoverfly/prod-api-version-test-file.json")));
    // This needs to be done to run tests faster. uncomment following line to capture the actual request.
    // rule.capture("prod-api-version-test-file.json");
    SslConfigurer sslConfigurer = rule.getSslConfigurer();

    OkHttpClient client =
        new OkHttpClient()
            .newBuilder()
            .sslSocketFactory(sslConfigurer.getSslContext().getSocketFactory(), sslConfigurer.getTrustManager())
            .build();
    Request request = new Request.Builder().url("https://app.harness.io/api/version").method("GET", null).build();
    Response response = client.newCall(request).execute();
    assertThat(response.code()).isEqualTo(200);
  }
}
