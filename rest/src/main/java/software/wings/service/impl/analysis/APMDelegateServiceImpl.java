package software.wings.service.impl.analysis;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;

import io.harness.network.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class APMDelegateServiceImpl implements APMDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(APMDelegateServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  private Map<String, String> decryptedFields = new HashMap<>();

  @Override
  public boolean validateCollector(APMValidateCollectorConfig config) {
    config.getHeaders().put("Accept", "application/json");
    final Call<Object> request =
        getAPMRestClient(config).validate(config.getUrl(), config.getHeaders(), config.getOptions());
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
  public String fetch(APMValidateCollectorConfig config) {
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

    final Call<Object> request = getAPMRestClient(config).collect(
        config.getUrl(), resolveDollarReferences(config.getHeaders()), resolveDollarReferences(config.getOptions()));
    final Response<Object> response;
    try {
      response = request.execute();
      if (response.isSuccessful()) {
        return JsonUtils.asJson(response.body());
      } else {
        logger.error("Request not successful. Reason: {}", response);
        throw new WingsException(response.errorBody().string());
      }
    } catch (Exception e) {
      throw new WingsException(e);
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
