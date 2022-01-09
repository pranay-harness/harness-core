/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.commandlibrary.client;

import static javax.ws.rs.core.Response.status;

import io.harness.exception.GeneralException;
import io.harness.network.SafeHttpCall;

import java.io.IOException;
import javax.ws.rs.core.Response.ResponseBuilder;
import lombok.experimental.UtilityClass;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@UtilityClass
public class CommandLibraryServiceClientUtils {
  public static <T> T executeHttpRequest(Call<T> call) {
    try {
      return SafeHttpCall.executeWithExceptions(call);
    } catch (IOException e) {
      throw new GeneralException("error while executing request", e);
    }
  }

  public static <T> javax.ws.rs.core.Response executeAndCreatePassThroughResponse(Call<T> call) {
    try {
      final Response<T> response = call.execute();
      if (response.isSuccessful()) {
        return createSuccessResponse(response);
      }
      return createFailureResponse(response);

    } catch (Exception e) {
      throw new GeneralException("error while processing request", e);
    }
  }

  private static <T> javax.ws.rs.core.Response createFailureResponse(Response<T> response) throws IOException {
    if (response.code() == 401) {
      return handleAuthError(response);
    } else {
      return handleDefaultError(response);
    }
  }

  private static <T> javax.ws.rs.core.Response handleDefaultError(Response<T> response) throws IOException {
    final ResponseBuilder responseBuilder = status(response.code());

    final ResponseBody errorBody = response.errorBody();
    if (errorBody != null) {
      responseBuilder.type(errorBody.contentType().toString()).entity(errorBody.string());
    }
    return responseBuilder.build();
  }

  private static <T> javax.ws.rs.core.Response handleAuthError(Response<T> response) throws IOException {
    final ResponseBody errorBody = response.errorBody();

    final String errorMessage = errorBody != null ? errorBody.string() : "";
    throw new GeneralException(
        String.format("auth error while connecting command library service with message : [%s] ", errorMessage));
  }

  private static <T> javax.ws.rs.core.Response createSuccessResponse(Response<T> response) {
    return status(response.code()).entity(response.body()).build();
  }
}
