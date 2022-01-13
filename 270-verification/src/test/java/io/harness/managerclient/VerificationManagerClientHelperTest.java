/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.managerclient;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.HttpServerErrorException;
import retrofit2.Call;
import retrofit2.Response;

@Slf4j
public class VerificationManagerClientHelperTest extends WingsBaseTest {
  private VerificationManagerClientHelper verificationManagerClientHelper;

  private Call<RestResponse<Boolean>> call;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    call = Mockito.mock(Call.class);
    verificationManagerClientHelper = Mockito.spy(VerificationManagerClientHelper.class);

    Request.Builder builder = new Request.Builder();
    builder.url("http://url.com");
    when(call.request()).thenReturn(builder.build());
    when(call.clone()).thenReturn(call);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCallManagerWithRetry_testMaxRetries() throws IOException {
    VerificationManagerClientHelper.INITIAL_DELAY_MS = 10;
    VerificationManagerClientHelper.JITTER = Duration.ofMillis(5);

    assertThatThrownBy(() -> verificationManagerClientHelper.callManagerWithRetry(call))
        .isInstanceOf(VerificationOperationException.class);
    verify(call, times(VerificationManagerClientHelper.MAX_ATTEMPTS)).execute();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCallManagerWithRetry_whenFirstCallSucceeds() throws IOException {
    VerificationManagerClientHelper.INITIAL_DELAY_MS = 10;
    VerificationManagerClientHelper.JITTER = Duration.ofMillis(5);
    Response response = Response.success(RestResponse.Builder.aRestResponse().withResource(true).build());
    when(call.execute()).thenReturn(response);
    RestResponse<Boolean> restResponse = verificationManagerClientHelper.callManagerWithRetry(call);
    assertThat(restResponse.getResource()).isTrue();
    verify(call, times(1)).execute();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCallManagerWithRetry_whenSecondCallSucceeds() throws IOException {
    VerificationManagerClientHelper.INITIAL_DELAY_MS = 10;
    VerificationManagerClientHelper.JITTER = Duration.ofMillis(5);
    Response response = Response.success(RestResponse.Builder.aRestResponse().withResource(true).build());
    doThrow(new RuntimeException("error message from test")).doReturn(response).when(call).execute();
    RestResponse<Boolean> restResponse = verificationManagerClientHelper.callManagerWithRetry(call);
    assertThat(restResponse.getResource()).isTrue();
    verify(call, times(2)).execute();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCallManagerWithRetry_whenSecondCallReturnsInternalServerError() throws IOException {
    VerificationManagerClientHelper.INITIAL_DELAY_MS = 10;
    VerificationManagerClientHelper.JITTER = Duration.ofMillis(5);
    doThrow(new RuntimeException("error message from test"))
        .doThrow(HttpServerErrorException.create(
            INTERNAL_SERVER_ERROR, "null pointer exception", HttpHeaders.EMPTY, null, null))
        .when(call)
        .execute();
    assertThatThrownBy(() -> verificationManagerClientHelper.callManagerWithRetry(call))
        .isInstanceOf(VerificationOperationException.class);
    verify(call, times(2)).execute();
  }
}
