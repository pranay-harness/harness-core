package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

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
}