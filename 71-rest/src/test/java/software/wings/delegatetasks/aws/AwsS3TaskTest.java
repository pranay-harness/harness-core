package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsS3ListBucketNamesRequest;
import software.wings.service.impl.aws.model.AwsS3Request;
import software.wings.service.intfc.aws.delegate.AwsS3HelperServiceDelegate;

public class AwsS3TaskTest extends WingsBaseTest {
  @Mock private AwsS3HelperServiceDelegate mockS3HelperServiceDelegate;

  @InjectMocks
  private AwsS3Task task = (AwsS3Task) TaskType.AWS_S3_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("s3HelperServiceDelegate", mockS3HelperServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRunWithTaskParameters() {
    AwsS3Request request = AwsS3ListBucketNamesRequest.builder().build();
    task.run(request);
    verify(mockS3HelperServiceDelegate).listBucketNames(any(), anyList());
  }

  @Test(expected = NotImplementedException.class)
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testRun() {
    AwsS3Request request = AwsS3ListBucketNamesRequest.builder().build();
    task.run(new Object[] {request});
  }
}