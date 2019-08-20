package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.URL_STRING;
import static software.wings.service.impl.ThirdPartyApiCallLog.PAYLOAD;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.APMVerificationState.Method;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class APMDelegateServiceImpl implements APMDelegateService {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  private Map<String, String> decryptedFields = new HashMap<>();

  @Override
  public boolean validateCollector(APMValidateCollectorConfig config) {
    config.getHeaders().put("Accept", "application/json");
    Call<Object> request = getAPMRestClient(config).validate(config.getUrl(), config.getHeaders(), config.getOptions());

    if (config.getCollectionMethod() != null && config.getCollectionMethod().equals(Method.POST)) {
      Map<String, Object> body = new HashMap<>();
      if (isNotEmpty(config.getBody())) {
        body = new JSONObject(config.getBody()).toMap();
      }
      request = getAPMRestClient(config).postCollect(config.getUrl(), resolveDollarReferences(config.getHeaders()),
          resolveDollarReferences(config.getOptions()), body);
    }

    final Response<Object> response;
    try {
      response = request.execute();
      if (response.isSuccessful()) {
        return true;
      } else {
        logger.error("Request not successful. Reason: {}", response);
        throw new WingsException(response.errorBody().string());
      }
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

  private BiMap<String, Object> resolveDollarReferences(Map<String, String> input) {
    BiMap<String, Object> output = HashBiMap.create();
    if (input == null) {
      return output;
    }
    for (Map.Entry<String, String> entry : input.entrySet()) {
      if (entry.getValue().startsWith("${")) {
        output.put(entry.getKey(), decryptedFields.get(entry.getValue().substring(2, entry.getValue().length() - 1)));
      } else {
        output.put(entry.getKey(), entry.getValue());
      }
    }

    return output;
  }

  @Override
  public String fetch(APMValidateCollectorConfig config, ThirdPartyApiCallLog apiCallLog) {
    config.getHeaders().put("Accept", "application/json");
    if (config.getEncryptedDataDetails() != null) {
      char[] decryptedValue;
      for (EncryptedDataDetail encryptedDataDetail : config.getEncryptedDataDetails()) {
        try {
          decryptedValue = encryptionService.getDecryptedValue(encryptedDataDetail);
          if (decryptedValue != null) {
            decryptedFields.put(encryptedDataDetail.getFieldName(), new String(decryptedValue));
          }
        } catch (IOException e) {
          throw new WingsException("APM fetch data : Unable to decrypt field " + encryptedDataDetail.getFieldName());
        }
      }
    }
    apiCallLog.setTitle("Fetching data from " + config.getUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

    Call<Object> request;
    if (config.getCollectionMethod() != null && config.getCollectionMethod().equals(Method.POST)) {
      Map<String, Object> body = new HashMap<>();
      if (isNotEmpty(config.getBody())) {
        body = new JSONObject(config.getBody()).toMap();
        apiCallLog.addFieldToRequest(
            ThirdPartyApiCallField.builder().name(PAYLOAD).value(JsonUtils.asJson(body)).type(FieldType.JSON).build());
      }
      request = getAPMRestClient(config).postCollect(config.getUrl(), resolveDollarReferences(config.getHeaders()),
          resolveDollarReferences(config.getOptions()), body);
    } else {
      request = getAPMRestClient(config).collect(
          config.getUrl(), resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()));
    }
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(URL_STRING)
                                     .value(request.request().url().toString())
                                     .type(FieldType.URL)
                                     .build());

    final Response<Object> response;
    try {
      response = request.execute();
      if (response.isSuccessful()) {
        apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
        delegateLogService.save(apiCallLog.getAccountId(), apiCallLog);
        return JsonUtils.asJson(response.body());
      } else {
        apiCallLog.addFieldToResponse(response.code(), response.errorBody(), FieldType.TEXT);
        delegateLogService.save(apiCallLog.getAccountId(), apiCallLog);
        logger.error("Request not successful. Reason: {}", response);
        throw new WingsException(response.errorBody().string());
      }
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(apiCallLog.getAccountId(), apiCallLog);
      logger.info("Error while getting response from metric provider", e);
      throw new WingsException("Unsuccessful response while fetching data from APM provider. Error message: "
          + e.getMessage() + " Request: " + request.request().url());
    }
  }

  private APMRestClient getAPMRestClient(final APMValidateCollectorConfig config) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(config.getBaseUrl())
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(Http.getOkHttpClientWithNoProxyValueSet(config.getBaseUrl())
                                              .connectTimeout(30, TimeUnit.SECONDS)
                                              .build())
                                  .build();
    return retrofit.create(APMRestClient.class);
  }
}
