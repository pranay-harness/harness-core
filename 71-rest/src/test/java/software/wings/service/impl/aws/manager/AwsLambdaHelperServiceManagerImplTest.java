package software.wings.service.impl.aws.manager;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.embed.AwsLambdaDetails;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsResponse;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.EncryptionService;

public class AwsLambdaHelperServiceManagerImplTest extends CategoryTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private DelegateService delegateService;

  @Spy @InjectMocks private AwsLambdaHelperServiceManagerImpl awsLambdaHelperServiceManager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getFunctionDetails() throws InterruptedException {
    final AwsLambdaDetailsRequest awsLambdaDetailsRequest =
        AwsLambdaDetailsRequest.builder().awsConfig(AwsConfig.builder().tag("tag").build()).build();
    final AwsLambdaDetails lambdaDetails = AwsLambdaDetails.builder().build();
    final AwsLambdaDetailsResponse awsLambdaDetailsResponse =
        AwsLambdaDetailsResponse.builder().executionStatus(ExecutionStatus.SUCCESS).details(lambdaDetails).build();
    doReturn(awsLambdaDetailsResponse).when(delegateService).executeTask(any(DelegateTask.class));

    Assertions.assertThat(awsLambdaHelperServiceManager.getFunctionDetails(awsLambdaDetailsRequest))
        .isEqualTo(lambdaDetails);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getFunctionDetails_error() throws InterruptedException {
    final AwsLambdaDetailsRequest awsLambdaDetailsRequest =
        AwsLambdaDetailsRequest.builder().awsConfig(AwsConfig.builder().tag("tag").build()).build();
    final AwsLambdaDetailsResponse awsLambdaDetailsResponse =
        AwsLambdaDetailsResponse.builder().executionStatus(ExecutionStatus.FAILED).errorMessage("error").build();
    doReturn(awsLambdaDetailsResponse).when(delegateService).executeTask(any(DelegateTask.class));

    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsLambdaHelperServiceManager.getFunctionDetails(awsLambdaDetailsRequest));

    doReturn(ErrorNotifyResponseData.builder().errorMessage("error").build())
        .when(delegateService)
        .executeTask(any(DelegateTask.class));
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsLambdaHelperServiceManager.getFunctionDetails(awsLambdaDetailsRequest));
  }
}