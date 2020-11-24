package io.harness.remote.client;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@UtilityClass
@Slf4j
public class NGRestUtils {
  public static <T> T getResponse(Call<ResponseDTO<T>> request) {
    try {
      Response<ResponseDTO<T>> response = request.execute();
      if (response.isSuccessful()) {
        return response.body().getData();
      } else {
        String errorMessage = "";
        try {
          ErrorDTO restResponse = JsonUtils.asObject(response.errorBody().string(), new TypeReference<ErrorDTO>() {});
          errorMessage = restResponse.getMessage();
        } catch (Exception e) {
          log.debug("Error while converting rest response to ErrorDTO", e);
        }
        throw new InvalidRequestException(
            StringUtils.isEmpty(errorMessage) ? "Error occurred while performing this operation" : errorMessage);
      }
    } catch (IOException ex) {
      log.error("IO error while connecting to manager", ex);
      throw new UnexpectedException("Unable to connect, please try again.");
    }
  }
}
