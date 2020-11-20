package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenRequest;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlRequest;
import software.wings.service.impl.aws.model.AwsEcrRequest;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;

public class AwsEcrTaskTest extends WingsBaseTest {
  @Mock private AwsEcrHelperServiceDelegate mockEcrServiceDelegate;

  @InjectMocks
  private AwsEcrTask task =
      new AwsEcrTask(DelegateTaskPackage.builder()
                         .delegateId("delegateid")
                         .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                         .build(),
          null, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("ecrServiceDelegate", mockEcrServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    AwsEcrRequest request = AwsEcrGetImageUrlRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEcrServiceDelegate).getEcrImageUrl(any(), anyList(), anyString(), anyString());
    request = AwsEcrGetAuthTokenRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEcrServiceDelegate).getAmazonEcrAuthToken(any(), anyList(), anyString(), anyString());
  }
}
