package io.harness.ccm.setup.service.support;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.app.MainConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class AwsCredentialHelperTest {
  @Mock MainConfiguration configuration;
  @InjectMocks AwsCredentialHelper awsCredentialHelper;

  private final String AWS_SECRET_KEY = "awsSecretKey";
  private final String AWS_ACCESS_KEY = "awsAccessKey";

  @Before
  public void setup() {
    when(configuration.getCeSetUpConfig())
        .thenReturn(CESetUpConfig.builder().awsAccessKey(AWS_ACCESS_KEY).awsSecretKey(AWS_SECRET_KEY).build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void constructAWSSecurityTokenServiceTest() {
    AWSSecurityTokenService awsSecurityTokenService = awsCredentialHelper.constructAWSSecurityTokenService();
    assertThat(awsSecurityTokenService).isNotNull();
  }
}