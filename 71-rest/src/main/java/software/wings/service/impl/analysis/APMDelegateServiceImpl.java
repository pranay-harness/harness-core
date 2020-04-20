package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.APMVerificationState.Method;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class APMDelegateServiceImpl implements APMDelegateService {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private RequestExecutor requestExecutor;

  private Map<String, String> decryptedFields = new HashMap<>();

  @Override
  public boolean validateCollector(APMValidateCollectorConfig config) {
    if (!config.getHeaders()
             .keySet()
             .stream()
             .map(String::toLowerCase)
             .collect(Collectors.toList())
             .contains("accept")) {
      config.getHeaders().put("Accept", "application/json");
    }
    decryptFields(config.getEncryptedDataDetails());
    Call<Object> request = getAPMRestClient(config).validate(
        resolveDollarReferences(config.getUrl()), config.getHeaders(), config.getOptions());
    if (config.getCollectionMethod() != null && config.getCollectionMethod() == Method.POST) {
      Map<String, Object> body = new HashMap<>();
      if (isNotEmpty(config.getBody())) {
        config.setBody(resolveDollarReferences(config.getBody()));
        body = new JSONObject(config.getBody()).toMap();
      }
      request = getAPMRestClient(config).postCollect(resolveDollarReferences(config.getUrl()),
          resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()), body);
    }

    requestExecutor.executeRequest(request);
    return true;
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
        throw new DataCollectionException("Could not resolve \"${" + fieldName + "}\" provided in input");
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
      String headerVal = entry.getValue();
      if (!headerVal.contains("${")) {
        output.put(entry.getKey(), entry.getValue());
        continue;
      }
      while (headerVal.contains("${")) {
        int startIndex = headerVal.indexOf("${");
        int endIndex = headerVal.indexOf('}', startIndex);
        String fieldName = headerVal.substring(startIndex + 2, endIndex);
        String headerBeforeIndex = headerVal.substring(0, startIndex);

        headerVal = headerBeforeIndex + decryptedFields.get(fieldName) + headerVal.substring(endIndex + 1);
        output.put(entry.getKey(), headerVal);
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
    if (!config.getHeaders()
             .keySet()
             .stream()
             .map(String::toLowerCase)
             .collect(Collectors.toList())
             .contains("accept")) {
      config.getHeaders().put("Accept", "application/json");
    }
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

    Call<Object> request;
    if (config.getCollectionMethod() != null && config.getCollectionMethod() == Method.POST) {
      Map<String, Object> body = new HashMap<>();
      if (isNotEmpty(config.getBody())) {
        config.setBody(resolveDollarReferences(config.getBody()));
        body = new JSONObject(config.getBody()).toMap();
      }
      request = getAPMRestClient(config).postCollect(resolveDollarReferences(config.getUrl()),
          resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()), body);
    } else {
      request = getAPMRestClient(config).collect(resolveDollarReferences(config.getUrl()),
          resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()));
    }

    return JsonUtils.asJson(requestExecutor.executeRequest(apiCallLog, request));
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
