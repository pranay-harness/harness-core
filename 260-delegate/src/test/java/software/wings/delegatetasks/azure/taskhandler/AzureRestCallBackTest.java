package software.wings.delegatetasks.azure.taskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static okhttp3.MediaType.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import javax.ws.rs.core.MediaType;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Response;

public class AzureRestCallBackTest extends WingsBaseTest {
  @Mock private ExecutionLogCallback logCallBack;

  String resourceGroupName = "resourceGroupName";
  private AzureRestCallBack<String> azureRestCallBack;

  @Before
  public void setup() {
    azureRestCallBack = new AzureRestCallBack<>(logCallBack, resourceGroupName);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testFailure() {
    azureRestCallBack.failure(new InvalidRequestException("Error message"));

    boolean updateFailed = azureRestCallBack.updateFailed();
    assertThat(updateFailed).isTrue();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testSuccess() {
    doNothing().when(logCallBack).saveExecutionLog(anyString(), any(), any());
    azureRestCallBack.success("Success");

    verify(logCallBack, times(1)).saveExecutionLog(anyString(), any(), any());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetErrorMessage() {
    azureRestCallBack.failure(new InvalidRequestException("Error message"));
    String errorMessage = azureRestCallBack.getErrorMessage();

    assertThat(errorMessage).isNotBlank();
    assertThat(errorMessage).isEqualTo("Error message");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testCloudExceptionErrorMessage() {
    CloudError cloudError = new CloudError();
    cloudError.withMessage("Reason of cloud error");
    azureRestCallBack.failure(new CloudException("Cloud exception error message",
        Response.error(500, ResponseBody.create(parse(MediaType.APPLICATION_JSON), "Reason of server error")),
        cloudError));
    String errorMessage = azureRestCallBack.getErrorMessage();

    assertThat(errorMessage).isNotBlank();
    assertThat(errorMessage).isEqualTo("Cloud exception error message: Reason of cloud error");
  }
}
