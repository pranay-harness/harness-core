/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.client;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;

public class RequestExecutorTest extends CategoryTest {
  private RequestExecutor requestExecutor;
  @Before
  public void setup() {
    requestExecutor = new RequestExecutor();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_whenSuccess() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    when(call.execute()).thenReturn(response);
    String returnedStr = requestExecutor.execute(call);
    assertThat(returnedStr).isEqualTo(responseStr);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_500ResponseNotRestResponse() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    Response<?> response = Response.error(ResponseBody.create(MediaType.parse("application/json"), "error"),
        new okhttp3.Response.Builder()
            .message("message")
            .code(500)
            .protocol(Protocol.HTTP_1_1)
            .request(new Request.Builder().url("http://localhost/").build())
            .build());
    when(call.execute()).thenReturn((Response<String>) response);
    assertThatThrownBy(() -> requestExecutor.execute(call))
        .isInstanceOf(ServiceCallException.class)
        .hasMessage("Response code: 500, Message: message, Error: error");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_500ResponseRestResponseWithoutStacktrace() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    RestResponse<?> restResponse = new RestResponse<>();
    restResponse.getResponseMessages().add(ResponseMessage.builder()
                                               .code(ErrorCode.DEFAULT_ERROR_CODE)
                                               .level(Level.ERROR)
                                               .message("error message from manager")
                                               .build());

    Response<?> response =
        Response.error(ResponseBody.create(MediaType.parse("application/json"), JsonUtils.asJson(restResponse)),
            new okhttp3.Response.Builder()
                .message("message")
                .code(500)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .build());
    when(call.execute()).thenReturn((Response<String>) response);
    assertThatThrownBy(() -> requestExecutor.execute(call))
        .isInstanceOf(ServiceCallException.class)
        .hasMessage("Response code: 500, Message: error message from manager")
        .hasNoCause();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_500ResponseRestResponseWithStacktrace() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    RestResponse<?> restResponse = new RestResponse<>();
    restResponse.getResponseMessages().add(ResponseMessage.builder()
                                               .code(ErrorCode.DEFAULT_ERROR_CODE)
                                               .level(Level.ERROR)
                                               .exception(new RuntimeException("exception cause from manager"))
                                               .message("error message from manager")
                                               .build());

    Response<?> response =
        Response.error(ResponseBody.create(MediaType.parse("application/json"), JsonUtils.asJson(restResponse)),
            new okhttp3.Response.Builder()
                .message("message")
                .code(500)
                .protocol(Protocol.HTTP_1_1)
                .request(new Request.Builder().url("http://localhost/").build())
                .build());
    when(call.execute()).thenReturn((Response<String>) response);
    assertThatThrownBy(() -> requestExecutor.execute(call))
        .isInstanceOf(ServiceCallException.class)
        .hasMessage("Response code: 500, Message: error message from manager")
        .hasCauseInstanceOf(Throwable.class)
        .getCause()
        .hasMessage("exception cause from manager");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_whenFailsWithIOException() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    IOException ioException = new IOException("io exception");
    when(call.execute()).thenThrow(ioException);
    assertThatThrownBy(() -> requestExecutor.execute(call))
        .isInstanceOf(ServiceCallException.class)
        .hasCause(ioException);
  }
}
