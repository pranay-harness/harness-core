package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.URL_STRING;
import static software.wings.service.impl.ThirdPartyApiCallLog.PAYLOAD;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Slf4j
public class APMDelegateServiceImpl implements APMDelegateService {
  private static final String DATADOG_API_MASK = "api_key=([^&]*)";
  private static final String DATADOG_APP_MASK = "application_key=([^&]*)";
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  private Map<String, String> decryptedFields = new HashMap<>();

  @Override
  public boolean validateCollector(APMValidateCollectorConfig config) {
    config.getHeaders().put("Accept", "application/json");
    Call<Object> request = getAPMRestClient(config).validate(
        resolveDollarReferences(config.getUrl()), config.getHeaders(), config.getOptions());
    decryptFields(config.getEncryptedDataDetails());
    if (config.getCollectionMethod() != null && config.getCollectionMethod() == Method.POST) {
      Map<String, Object> body = new HashMap<>();
      if (isNotEmpty(config.getBody())) {
        config.setBody(resolveDollarReferences(config.getBody()));
        body = new JSONObject(config.getBody()).toMap();
      }
      request = getAPMRestClient(config).postCollect(resolveDollarReferences(config.getUrl()),
          resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()), body);
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

  private String resolveDollarReferences(String input) {
    if (input == null) {
      return null;
    }
    while (input.contains("${")) {
      int startIndex = input.indexOf("${");
      int endIndex = input.indexOf('}', startIndex);
      String fieldName = input.substring(startIndex + 2, endIndex);
      String headerBeforeIndex = input.substring(0, startIndex);
      if (!decryptedFields.containsKey(fieldName)) {
        throw new VerificationOperationException(ErrorCode.APM_CONFIGURATION_ERROR, "Invalid secret provided in input");
      }
      input = headerBeforeIndex + decryptedFields.get(fieldName) + input.substring(endIndex + 1);
    }
    return input;
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

  private void decryptFields(List<EncryptedDataDetail> encryptedDataDetails) {
    if (isNotEmpty(encryptedDataDetails)) {
      char[] decryptedValue;
      for (EncryptedDataDetail encryptedDataDetail : encryptedDataDetails) {
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
          throw new VerificationOperationException(ErrorCode.APM_CONFIGURATION_ERROR,
              "APM fetch data : Unable to decrypt field " + encryptedDataDetail.getFieldName());
        }
      }
    }
    apiCallLog.setTitle("Fetching data from " + config.getUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

    Call<Object> request;
    if (config.getCollectionMethod() != null && config.getCollectionMethod() == Method.POST) {
      Map<String, Object> body = new HashMap<>();
      if (isNotEmpty(config.getBody())) {
        String bodyToLog = config.getBody();
        config.setBody(resolveDollarReferences(config.getBody()));
        body = new JSONObject(config.getBody()).toMap();
        apiCallLog.addFieldToRequest(
            ThirdPartyApiCallField.builder().name(PAYLOAD).value(bodyToLog).type(FieldType.JSON).build());
      }
      request = getAPMRestClient(config).postCollect(resolveDollarReferences(config.getUrl()),
          resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()), body);
    } else {
      request = getAPMRestClient(config).collect(resolveDollarReferences(config.getUrl()),
          resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()));
    }
    String urlToLog = request.request().url().toString();
    if (urlToLog.contains("api_key")) {
      Pattern batchPattern = Pattern.compile(DATADOG_API_MASK);
      Matcher matcher = batchPattern.matcher(urlToLog);
      while (matcher.find()) {
        final String apiKey = matcher.group(1);
        urlToLog = urlToLog.replace(apiKey, "<apiKey>");
      }
    }

    if (urlToLog.contains("application_key")) {
      Pattern batchPattern = Pattern.compile(DATADOG_APP_MASK);
      Matcher matcher = batchPattern.matcher(urlToLog);
      while (matcher.find()) {
        final String appKey = matcher.group(1);
        urlToLog = urlToLog.replace(appKey, "<appKey>");
      }
    }

    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(URL_STRING).value(urlToLog).type(FieldType.URL).build());

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
