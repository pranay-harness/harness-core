/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.network;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.exception.HttpResponseException;

import java.io.IOException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
@Slf4j
public class SafeHttpCall {
  public static <T> T execute(Call<T> call) throws IOException {
    Response<T> response = null;
    try {
      response = call.execute();
      printErrorResponse(response);
      return response.body();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }

  public static <T> T executeWithExceptions(Call<T> call) throws IOException {
    Response<T> response = null;
    try {
      response = call.execute();
      validateResponse(response);
      return response.body();
    } finally {
      if (response != null && !response.isSuccessful()) {
        response.errorBody().close();
      }
    }
  }

  public static void validateResponse(Response<?> response) throws IOException {
    okhttp3.Response rawResponse = response == null ? null : response.raw();
    if (rawResponse == null) {
      throw new GeneralException("Null response found");
    }
    if (!rawResponse.isSuccessful()) {
      throw new HttpResponseException(rawResponse.code(), getErrorMessage(response, rawResponse));
    }
  }

  private static String getErrorMessage(Response<?> response, okhttp3.Response rawResponse) throws IOException {
    if (StringUtils.isNotBlank(rawResponse.message())) {
      return rawResponse.message();
    }
    if (response.errorBody() == null) {
      return "";
    }
    return response.errorBody().string();
  }

  public static void printErrorResponse(Response<?> response) {
    if (response == null) {
      log.info("Null response found");
    }
    if (!response.isSuccessful()) {
      log.info(format("Unsuccessful HTTP call: status code = %d, message = %s, errorbody = %s", response.code(),
          response.message() == null ? "null" : response.message(), response.errorBody()));
    }
  }
}
