package io.harness.delegate.task.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ServiceNowException;
import io.harness.network.Http;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import software.wings.helpers.ext.servicenow.ServiceNowRestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ServiceNowTaskNgHelper {
  private final SecretDecryptionService secretDecryptionService;
  static final long TIME_OUT = 60;

  @Inject
  public ServiceNowTaskNgHelper(SecretDecryptionService secretDecryptionService) {
    this.secretDecryptionService = secretDecryptionService;
  }

  public ServiceNowTaskNGResponse getServiceNowResponse(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    decryptRequestDTOs(serviceNowTaskNGParameters);
    switch (serviceNowTaskNGParameters.getAction()) {
      case VALIDATE_CREDENTIALS:
        return validateCredentials(serviceNowTaskNGParameters);
      default:
        throw new InvalidRequestException(
            String.format("Invalid servicenow task action: %s", serviceNowTaskNGParameters.getAction()));
    }
  }

  private ServiceNowTaskNGResponse validateCredentials(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String url = serviceNowConnectorDTO.getServiceNowUrl();
    String userName = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
        serviceNowConnectorDTO.getUsername(), serviceNowConnectorDTO.getUsernameRef());
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    Retrofit retrofit = new Retrofit.Builder()
                            .client(getHttpClient(url, userName, password))
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    ServiceNowRestClient serviceNowRestClient = retrofit.create(ServiceNowRestClient.class);
    final Call<JsonNode> request = serviceNowRestClient.validateConnection(Credentials.basic(userName, password));
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      handleResponse(response, "Failed to validate ServiceNow credentials");
      return ServiceNowTaskNGResponse.builder().build();
    } catch (Exception e) {
      log.error("Failed to authenticate to servicenow. ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  @NotNull
  private OkHttpClient getHttpClient(String url, String userName, String password) {
    return getOkHttpClientBuilder()
        .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
        .readTimeout(TIME_OUT, TimeUnit.SECONDS)
        .proxy(Http.checkAndGetNonProxyIfApplicable(url))
        .addInterceptor(chain -> {
          Request newRequest =
              chain.request().newBuilder().addHeader("Authorization", Credentials.basic(userName, password)).build();
          return chain.proceed(newRequest);
        })
        .build();
  }

  private void decryptRequestDTOs(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    secretDecryptionService.decrypt(
        serviceNowTaskNGParameters.getServiceNowConnectorDTO(), serviceNowTaskNGParameters.getEncryptionDetails());
  }

  public static void handleResponse(Response<?> response, String message) throws IOException {
    if (response.isSuccessful()) {
      return;
    }
    if (response.code() == 401) {
      throw new ServiceNowException("Invalid ServiceNow credentials", SERVICENOW_ERROR, USER);
    }
    if (response.code() == 404) {
      throw new ServiceNowException("404 Not found", SERVICENOW_ERROR, USER);
    }
    if (response.errorBody() == null) {
      throw new ServiceNowException(message + " : " + response.message(), SERVICENOW_ERROR, USER);
    }
    throw new ServiceNowException(response.errorBody().string(), SERVICENOW_ERROR, USER);
  }
}
