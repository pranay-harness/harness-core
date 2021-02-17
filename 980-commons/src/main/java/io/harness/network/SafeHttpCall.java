package io.harness.network;

import static java.lang.String.format;

import io.harness.exception.GeneralException;

import java.io.IOException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

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

  public static void validateResponse(Response<?> response) {
    validateRawResponse(response == null ? null : response.raw());
  }

  public static void validateRawResponse(okhttp3.Response response) {
    if (response == null) {
      throw new GeneralException("Null response found");
    }
    if (!response.isSuccessful()) {
      throw new GeneralException(format("Unsuccessful HTTP call: status code = %d, message = %s", response.code(),
          response.message() == null ? "null" : response.message()));
    }
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
