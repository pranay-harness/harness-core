package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AwsHelperServiceDelegateBaseTest extends WingsBaseTest {
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
}
