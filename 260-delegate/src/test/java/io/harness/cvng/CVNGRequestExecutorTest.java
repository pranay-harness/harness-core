/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.stubbing.Answer;
import retrofit2.Call;
import retrofit2.Response;

public class CVNGRequestExecutorTest extends CategoryTest {
  private CVNGRequestExecutor cvngRequestExecutor;
  @Before
  public void setUp() throws Exception {
    cvngRequestExecutor = new CVNGRequestExecutor();
    FieldUtils.writeField(cvngRequestExecutor, "executorService", Executors.newFixedThreadPool(1), true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecute_successfulResponse() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    when(call.execute()).thenReturn(response);
    assertThat(cvngRequestExecutor.execute(call)).isEqualTo(responseStr);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testExecuteWithTimeout() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    when(call.execute()).thenAnswer((Answer<Response<String>>) invocation -> {
      try {
        Thread.sleep(7);
      } catch (InterruptedException e) {
        throw new IllegalStateException(e);
      }
      return response;
    });
    assertThatThrownBy(() -> cvngRequestExecutor.executeWithTimeout(call, Duration.of(5, ChronoUnit.MILLIS)))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(TimeoutException.class);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void executeWithRetry_successAfterOneFailure() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    when(call.execute()).thenThrow(new RuntimeException("error")).thenReturn(response);
    assertThat(cvngRequestExecutor.executeWithRetry(call)).isEqualTo(responseStr);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void executeWithRetry_allFailures() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    String responseStr = "This is test response";
    Response<String> response = Response.success(responseStr);
    RuntimeException runtimeException = new RuntimeException("error");
    when(call.execute()).thenThrow(runtimeException);
    assertThatThrownBy(() -> cvngRequestExecutor.executeWithRetry(call))
        .isInstanceOf(IllegalStateException.class)
        .hasCause(runtimeException);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void execute_non200Response() throws IOException {
    Request request = new Request.Builder().url("http://example.com/test").build();
    Call<String> call = mock(Call.class);
    when(call.clone()).thenReturn(call);
    when(call.request()).thenReturn(request);
    Response<String> response = internalServerErrorResponse("Unable to update status");
    when(call.execute()).thenReturn(response);
    assertThatThrownBy(() -> cvngRequestExecutor.execute(call))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "java.lang.IllegalStateException: Response Code: 500, Response Message: test, Error Body: Unable to update status");
  }

  private Response internalServerErrorResponse(String responseStr) {
    return Response.error(ResponseBody.create(MediaType.parse("text/plain"), responseStr),
        new okhttp3.Response.Builder()
            .code(500)
            .protocol(Protocol.HTTP_1_1)
            .message("test")
            .request(new Request.Builder().url("http://localhost/").build())
            .build());
  }
}
