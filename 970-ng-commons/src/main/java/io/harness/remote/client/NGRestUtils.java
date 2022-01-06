/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.remote.client;

import static io.harness.remote.client.RestClientUtils.DEFAULT_CONNECTION_ERROR_MESSAGE;
import static io.harness.remote.client.RestClientUtils.DEFAULT_ERROR_MESSAGE;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@UtilityClass
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NGRestUtils {
  private final int MAX_ATTEMPTS = 3;
  public static <T> T getResponse(Call<ResponseDTO<T>> request) {
    return getResponse(request, DEFAULT_ERROR_MESSAGE, DEFAULT_CONNECTION_ERROR_MESSAGE);
  }

  public static <T> T getResponse(Call<ResponseDTO<T>> request, String defaultErrorMessage) {
    return getResponse(request, defaultErrorMessage, DEFAULT_CONNECTION_ERROR_MESSAGE);
  }

  public static <T> T getResponseWithRetry(Call<ResponseDTO<T>> request) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicy(MAX_ATTEMPTS, format("Failed to connect to upstream system after {} attempts"),
            format("Unable to connect to upstream systems after {} attempts."));
    return Failsafe.with(retryPolicy)
        .get(() -> getResponse(request.clone(), DEFAULT_ERROR_MESSAGE, DEFAULT_CONNECTION_ERROR_MESSAGE));
  }

  public static <T> T getResponseWithRetry(Call<ResponseDTO<T>> request, String defaultErrorMessage) {
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        MAX_ATTEMPTS, format(defaultErrorMessage), format("Unable to connect to upstream systems after {} attempts."));
    return Failsafe.with(retryPolicy)
        .get(() -> getResponse(request.clone(), defaultErrorMessage, DEFAULT_CONNECTION_ERROR_MESSAGE));
  }

  public static <T> T getResponseWithRetry(Call<ResponseDTO<T>> request, int maxAttempts) {
    RetryPolicy<Object> retryPolicy =
        getRetryPolicy(maxAttempts, format("Failed to connect to upstream system after {} attempts"),
            format("Unable to connect to upstream systems after {} attempts."));
    return Failsafe.with(retryPolicy)
        .get(() -> getResponse(request.clone(), DEFAULT_ERROR_MESSAGE, DEFAULT_CONNECTION_ERROR_MESSAGE));
  }

  public static <T> T getResponseWithRetry(Call<ResponseDTO<T>> request, String defaultErrorMessage, int maxAttempts) {
    RetryPolicy<Object> retryPolicy = getRetryPolicy(
        maxAttempts, format(defaultErrorMessage), format("Unable to connect to upstream systems after {} attempts."));
    return Failsafe.with(retryPolicy)
        .get(() -> getResponse(request.clone(), defaultErrorMessage, DEFAULT_CONNECTION_ERROR_MESSAGE));
  }
  public static <T> T getResponse(
      Call<ResponseDTO<T>> request, String defaultErrorMessage, String connectionErrorMessage) {
    try {
      Response<ResponseDTO<T>> response = request.execute();
      if (response.isSuccessful()) {
        return response.body().getData();
      } else {
        log.error("Error Response received: {}", response);
        String errorMessage = "";
        InvalidRequestException invalidRequestException = null;
        try {
          ErrorDTO restResponse = JsonUtils.asObject(response.errorBody().string(), new TypeReference<ErrorDTO>() {});
          errorMessage = restResponse.getMessage();
          invalidRequestException = new InvalidRequestException(
              StringUtils.isEmpty(errorMessage) ? defaultErrorMessage : errorMessage, restResponse.getMetadata());
        } catch (Exception e) {
          log.error("Error while converting rest response to ErrorDTO", e);
          invalidRequestException =
              new InvalidRequestException(StringUtils.isEmpty(errorMessage) ? defaultErrorMessage : errorMessage);
        }
        throw invalidRequestException;
      }
    } catch (IOException ex) {
      String url = Optional.ofNullable(request.request()).map(x -> x.url().encodedPath()).orElse(null);
      log.error("IO error while connecting to the service: {}", url, ex);
      throw new UnexpectedException(connectionErrorMessage);
    }
  }
  private RetryPolicy<Object> getRetryPolicy(int maxAttempts, String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withMaxAttempts(maxAttempts)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
