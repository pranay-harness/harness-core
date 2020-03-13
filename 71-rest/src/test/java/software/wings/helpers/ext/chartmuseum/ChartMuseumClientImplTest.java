package software.wings.helpers.ext.chartmuseum;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;

import java.util.Map;

public class ChartMuseumClientImplTest extends WingsBaseTest {
  @InjectMocks private ChartMuseumClientImpl chartMuseumClient = Mockito.spy(ChartMuseumClientImpl.class);

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getEnvForAwsConfig() {
    testGetEnvForAwsConfig();
    testGetEnvForAwsConfigWithAssumeDelegateRole();
  }

  private void testGetEnvForAwsConfigWithAssumeDelegateRole() {
    AwsConfig awsConfig = AwsConfig.builder().useEc2IamCredentials(true).build();
    Map<String, String> env = chartMuseumClient.getEnvForAwsConfig(awsConfig);
    assertThat(env).isEmpty();
  }

  private void testGetEnvForAwsConfig() {
    AwsConfig awsConfig = AwsConfig.builder().accessKey("abc").secretKey("topSecret".toCharArray()).build();
    Map<String, String> env = chartMuseumClient.getEnvForAwsConfig(awsConfig);
    assertThat(env.get(ChartMuseumConstants.AWS_ACCESS_KEY_ID)).isEqualTo(awsConfig.getAccessKey());
    assertThat(env.get(ChartMuseumConstants.AWS_SECRET_ACCESS_KEY)).isEqualTo(new String(awsConfig.getSecretKey()));
    assertThat(env.keySet()).hasSize(2);
  }
}