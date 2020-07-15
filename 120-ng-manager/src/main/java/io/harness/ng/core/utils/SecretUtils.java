package io.harness.ng.core.utils;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;

import io.harness.rest.RestResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.service.impl.security.SecretManagementException;

import java.io.IOException;

@UtilityClass
@Slf4j
public class SecretUtils {
  public static <T> T getResponse(Call<RestResponse<T>> request) {
    try {
      Response<RestResponse<T>> response = request.execute();
      if (response != null) {
        if (response.isSuccessful()) {
          return response.body().getResource();
        } else {
          logger.error("Error while connecting to manager: {}", response.errorBody());
        }
      }
    } catch (IOException ex) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR, "Unable to connect", ex, USER);
    }

    throw new SecretManagementException(
        SECRET_MANAGEMENT_ERROR, "Some error occurred, please contact Harness support.", USER);
  }
}
