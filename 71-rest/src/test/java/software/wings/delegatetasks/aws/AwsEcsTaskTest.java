package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.ecs.model.Service;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsEcsListClusterServicesRequest;
import software.wings.service.impl.aws.model.AwsEcsListClusterServicesResponse;
import software.wings.service.impl.aws.model.AwsEcsListClustersRequest;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.Collections;

public class AwsEcsTaskTest extends WingsBaseTest {
  @Mock private AwsEcsHelperServiceDelegate mockEcsHelperServiceDelegate;

  @InjectMocks
  private AwsEcsTask task = (AwsEcsTask) TaskType.AWS_ECS_TASK.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delegateid")
          .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
          .build(),
      null, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("ecsHelperServiceDelegate", mockEcsHelperServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    AwsEcsRequest request = AwsEcsListClustersRequest.builder().build();
    task.run(request);
    verify(mockEcsHelperServiceDelegate).listClusters(any(), anyList(), anyString());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListServicesForCluster() {
    Service service = new Service();
    service.setServiceName("serviceName");
    doReturn(Collections.singletonList(service))
        .when(mockEcsHelperServiceDelegate)
        .listServicesForCluster(any(), any(), anyString(), anyString());
    AwsEcsListClusterServicesRequest awsEcsListClusterServicesRequest =
        AwsEcsListClusterServicesRequest.builder().build();

    AwsResponse awsResponse = task.run(awsEcsListClusterServicesRequest);

    assertThat(awsResponse instanceof AwsEcsListClusterServicesResponse).isTrue();
    assertThat(((AwsEcsListClusterServicesResponse) awsResponse).getExecutionStatus())
        .isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(((AwsEcsListClusterServicesResponse) awsResponse).getServices().get(0).getServiceName())
        .isEqualTo("serviceName");
    verify(mockEcsHelperServiceDelegate).listServicesForCluster(any(), any(), anyString(), anyString());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testRunWithInvalidRequestException() {
    doThrow(new RuntimeException("Error msg"))
        .when(mockEcsHelperServiceDelegate)
        .listServicesForCluster(any(), any(), anyString(), anyString());
    AwsEcsListClusterServicesRequest awsEcsListClusterServicesRequest =
        AwsEcsListClusterServicesRequest.builder().build();

    AwsResponse awsResponse = task.run(awsEcsListClusterServicesRequest);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> task.run(awsEcsListClusterServicesRequest));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testRunWithWingsException() {
    doThrow(new InvalidRequestException("Error msg"))
        .when(mockEcsHelperServiceDelegate)
        .listServicesForCluster(any(), any(), anyString(), anyString());
    AwsEcsListClusterServicesRequest awsEcsListClusterServicesRequest =
        AwsEcsListClusterServicesRequest.builder().build();

    AwsResponse awsResponse = task.run(awsEcsListClusterServicesRequest);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> task.run(awsEcsListClusterServicesRequest));
  }
}
