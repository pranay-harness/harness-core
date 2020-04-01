package io.harness.network;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.rule.Owner;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

public class SafeHttpCallTest extends CategoryTest {
  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testNullResponseExecuteWithExceptions() throws IOException {
    Call<String> call = spy(Call.class);
    doReturn(null).when(call).execute();
    SafeHttpCall.executeWithExceptions(call);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testSuccessfulExecuteWithExceptions() throws IOException {
    String msg = "hello";
    Call<String> call = spy(Call.class);
    doReturn(Response.success(msg)).when(call).execute();
    String retVal = SafeHttpCall.executeWithExceptions(call);
    assertThat(retVal).isEqualTo(msg);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUnsuccessfulExecuteWithExceptions() throws IOException {
    Call<String> call = spy(Call.class);
    doReturn(prepareErrorResponse(501)).when(call).execute();
    SafeHttpCall.executeWithExceptions(call);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidateNullResponse() {
    SafeHttpCall.validateResponse(null);
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidateSuccessfulResponse() {
    SafeHttpCall.validateResponse(Response.success("hello"));
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidateUnsuccessfulResponse() {
    SafeHttpCall.validateResponse(prepareErrorResponse(401));
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidateNullRawResponse() {
    SafeHttpCall.validateRawResponse(null);
  }

  @Test(expected = Test.None.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidateSuccessfulRawResponse() {
    SafeHttpCall.validateRawResponse(prepareRawResponse(200));
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testValidateUnsuccessfulRawResponse() {
    SafeHttpCall.validateRawResponse(prepareRawResponse(401));
  }

  private Response<String> prepareErrorResponse(int code) {
    return Response.error(code, ResponseBody.create(MediaType.parse("text/plain"), "MSG"));
  }

  private okhttp3.Response prepareRawResponse(int code) {
    Request request = new Request.Builder().url("https://dummyurl.com").method("GET", null).build();
    return new okhttp3.Response.Builder()
        .code(code)
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .message("MSG")
        .build();
  }
}
