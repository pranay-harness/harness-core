/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.remote.client;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.remote.client.RestClientUtils.DEFAULT_CONNECTION_ERROR_MESSAGE;
import static io.harness.remote.client.RestClientUtils.DEFAULT_ERROR_MESSAGE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.rest.RestResponse;
import io.harness.serializer.JsonUtils;
import io.harness.serializer.KryoSerializer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(PL)
@Singleton
@Slf4j
public class RestClientExecutor {
  private static final MediaType APPLICATION_KRYO_MEDIA_TYPE = MediaType.parse("application/x-kryo");
  @Inject private KryoSerializer kryoSerializer;

  public <T> T getResponse(Call<RestResponse<T>> request) {
    return getResponse(request, DEFAULT_ERROR_MESSAGE, DEFAULT_CONNECTION_ERROR_MESSAGE);
  }

  public <T> T getResponse(Call<RestResponse<T>> request, String defaultErrorMessage) {
    return getResponse(request, defaultErrorMessage, DEFAULT_CONNECTION_ERROR_MESSAGE);
  }

  public <T> T getResponse(Call<RestResponse<T>> request, String defaultErrorMessage, String connectionErrorMessage) {
    try {
      Response<RestResponse<T>> response = request.execute();
      if (response.isSuccessful()) {
        return response.body().getResource();
      } else {
        String errorMessage = "";
        try {
          RestResponse<T> restResponse;
          if (response.errorBody().contentType().toString().startsWith(APPLICATION_KRYO_MEDIA_TYPE.toString())) {
            byte[] bytes = response.errorBody().bytes();
            restResponse = (RestResponse<T>) kryoSerializer.asObject(bytes);
          } else {
            restResponse = JsonUtils.asObject(response.errorBody().string(), new TypeReference<RestResponse<T>>() {});
          }
          if (restResponse != null && isNotEmpty(restResponse.getResponseMessages())) {
            List<ResponseMessage> responseMessageList = restResponse.getResponseMessages();
            errorMessage = responseMessageList.get(0).getMessage();
          }
        } catch (Exception e) {
          log.debug("Error while converting error received from upstream systems", e);
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
