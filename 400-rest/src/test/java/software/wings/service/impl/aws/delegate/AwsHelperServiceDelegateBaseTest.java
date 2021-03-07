package software.wings.service.impl.aws.delegate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.AwsHelperService;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.RAGHVENDRA;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WebIdentityTokenCredentialsProvider.class})
@PowerMockIgnore({"javax.security.*", "javax.net.*", "javax.management.*", "javax.crypto.*"})
public class AwsHelperServiceDelegateBaseTest extends WingsBaseTest {
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAmazonClientException() {
    AwsHelperServiceDelegateBase delegateBase = spy(AwsHelperServiceDelegateBase.class);
    AmazonClientException exception = new AmazonClientException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonClientException(exception))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testHandleAmazonServiceException() {
    AwsHelperServiceDelegateBase delegateBase = spy(AwsHelperServiceDelegateBase.class);
    AmazonServiceException exception1 = new AmazonCodeDeployException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception1))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception2 = new AmazonEC2Exception("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception2))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception3 = new ClusterNotFoundException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception3))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception4 = new ServiceNotFoundException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception4))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception5 = new AmazonECSException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception5))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception6 = new AWSLambdaException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception6))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetRegion() {
    AwsHelperServiceDelegateBase delegateBase = spy(AwsHelperServiceDelegateBase.class);
    AwsConfig config = AwsConfig.builder().build();
    assertThat(delegateBase.getRegion(config)).isEqualTo(AWS_DEFAULT_REGION);
    config.setDefaultRegion(Regions.US_GOV_EAST_1.getName());
    assertThat(delegateBase.getRegion(config)).isEqualTo(Regions.US_GOV_EAST_1.getName());
  }

  @Test(expected = com.amazonaws.SdkClientException.class)
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testAttachCredentialsAndBackoffPolicyWithIRSA() {
    AwsHelperService awsHelperService = new AwsHelperService();
    PowerMockito.mockStatic(System.class);
    PowerMockito.when(System.getenv(SDKGlobalConfiguration.AWS_ROLE_ARN_ENV_VAR)).thenReturn("abcd");
    PowerMockito.when(System.getenv(SDKGlobalConfiguration.AWS_WEB_IDENTITY_ENV_VAR)).thenReturn("/jkj");
    AwsConfig awsConfig = mock(AwsConfig.class);
    when(awsConfig.isUseEc2IamCredentials()).thenReturn(false);
    when(awsConfig.isUseIRSA()).thenReturn(true);
    when(awsConfig.isAssumeCrossAccountRole()).thenReturn(false);
    AwsClientBuilder awsClientBuilder = AmazonEC2ClientBuilder.standard().withRegion("us-east-1");

    awsHelperService.attachCredentialsAndBackoffPolicy(awsClientBuilder, awsConfig);

    assertThat(awsClientBuilder.getCredentials()).isInstanceOf(WebIdentityTokenCredentialsProvider.class);
    awsClientBuilder.getCredentials().getCredentials().getAWSSecretKey();
  }
}
