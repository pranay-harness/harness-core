package io.harness.expression.configuration.util;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.expression.ExpressionServiceTestBase;
import io.harness.expression.app.ExpressionServiceConfiguration;
import io.harness.grpc.server.Connector;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.io.InputStream;

public class ExpressionServiceConfigurationUtilsTest extends ExpressionServiceTestBase {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldReadConfigurationFile() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    InputStream config = classLoader.getResourceAsStream("test-config.yml");
    ExpressionServiceConfiguration applicationConfiguration =
        ExpressionServiceConfigurationUtils.getApplicationConfiguration(config);
    assertThat(applicationConfiguration.getConnectors())
        .hasSize(2)
        .containsExactlyInAnyOrder(
            Connector.builder().port(9901).secure(true).certFilePath("cert.pem").keyFilePath("key.pem").build(),
            Connector.builder().port(9902).secure(false).build());
    assertThat(applicationConfiguration.getSecret()).isEqualTo("secret");
  }
}