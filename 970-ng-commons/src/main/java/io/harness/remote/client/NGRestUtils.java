package io.harness.remote.client;

import static io.harness.remote.client.RestClientUtils.DEFAULT_CONNECTION_ERROR_MESSAGE;
import static io.harness.remote.client.RestClientUtils.DEFAULT_ERROR_MESSAGE;

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
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@UtilityClass
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NGRestUtils {
  public static <T> T getResponse(Call<ResponseDTO<T>> request) {
    return getResponse(request, DEFAULT_ERROR_MESSAGE, DEFAULT_CONNECTION_ERROR_MESSAGE);
  }

  public static <T> T getResponse(Call<ResponseDTO<T>> request, String defaultErrorMessage) {
    return getResponse(request, defaultErrorMessage, DEFAULT_CONNECTION_ERROR_MESSAGE);
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
        try {
          ErrorDTO restResponse = JsonUtils.asObject(response.errorBody().string(), new TypeReference<ErrorDTO>() {});
          errorMessage = restResponse.getMessage();
        } catch (Exception e) {
          log.error("Error while converting rest response to ErrorDTO", e);
        }
        throw new InvalidRequestException(StringUtils.isEmpty(errorMessage) ? defaultErrorMessage : errorMessage);
      }
    } catch (IOException ex) {
      String url = Optional.ofNullable(request.request()).map(x -> x.url().encodedPath()).orElse(null);
      log.error("IO error while connecting to the service: {}", url, ex);
      throw new UnexpectedException(connectionErrorMessage);
    }
  }
}
