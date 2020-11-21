package io.harness.remote.client;

import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.rest.RestResponse;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@UtilityClass
@Slf4j
public class RestClientUtils {
  public static <T> T getResponse(Call<RestResponse<T>> request) {
    try {
      Response<RestResponse<T>> response = request.execute();
      if (response.isSuccessful()) {
        return response.body().getResource();
      } else {
        String errorMessage = "";
        try {
          RestResponse<T> restResponse =
              JsonUtils.asObject(response.errorBody().string(), new TypeReference<RestResponse<T>>() {});
          if (!restResponse.getResponseMessages().isEmpty()) {
            List<ResponseMessage> responseMessageList = restResponse.getResponseMessages();
            errorMessage = responseMessageList.get(0).getMessage();
          }
        } catch (Exception e) {
          log.debug("Error while converting error received from 71 rest manager", e);
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
