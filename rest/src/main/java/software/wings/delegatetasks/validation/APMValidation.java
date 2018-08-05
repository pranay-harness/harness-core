package software.wings.delegatetasks.validation;

import static java.util.Collections.singletonList;

import io.harness.network.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class APMValidation extends AbstractSecretManagerValidation {
  private static final Logger logger = LoggerFactory.getLogger(APMValidation.class);

  public APMValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o
                                 -> o instanceof DatadogConfig || o instanceof APMVerificationConfig
                                     || o instanceof APMValidateCollectorConfig || o instanceof APMDataCollectionInfo)
                             .map(obj -> {
                               if (obj instanceof DatadogConfig) {
                                 DatadogConfig config = (DatadogConfig) obj;
                                 return config.getUrl() + DatadogConfig.validationUrl;
                               } else if (obj instanceof APMVerificationConfig) {
                                 APMVerificationConfig config = (APMVerificationConfig) obj;
                                 return config.getUrl() + config.getValidationUrl();
                               } else if (obj instanceof APMDataCollectionInfo) {
                                 APMDataCollectionInfo dInfo = (APMDataCollectionInfo) obj;
                                 return dInfo.getBaseUrl() + dInfo.getValidationUrl();
                               } else {
                                 APMValidateCollectorConfig config = (APMValidateCollectorConfig) obj;
                                 return config.getBaseUrl() + config.getUrl();
                               }
                             })
                             .findFirst()
                             .orElse(null));
  }

  @Override
  protected EncryptionConfig getEncryptionConfig() {
    for (Object parmeter : getParameters()) {
      if (parmeter instanceof APMDataCollectionInfo) {
        return ((APMDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
      }
    }
    return super.getEncryptionConfig();
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    DelegateConnectionResult delegateConnectionResult = validateSecretManager();
    if (!delegateConnectionResult.isValidated()) {
      delegateConnectionResult.setCriteria(getCriteria().get(0));
      return singletonList(delegateConnectionResult);
    }

    Object[] configList = getParameters();
    APMValidateCollectorConfig validateCollectorConfig = null;
    for (Object config : configList) {
      if (config instanceof DatadogConfig) {
        DatadogConfig datadogConfig = (DatadogConfig) config;
        validateCollectorConfig = datadogConfig.createAPMValidateCollectorConfig();
        break;
      } else if (config instanceof APMVerificationConfig) {
        APMVerificationConfig apmConfig = (APMVerificationConfig) config;
        validateCollectorConfig = APMValidateCollectorConfig.builder()
                                      .baseUrl(apmConfig.getUrl())
                                      .url(apmConfig.getValidationUrl())
                                      .options(apmConfig.collectionHeaders())
                                      .headers(apmConfig.collectionParams())
                                      .build();
        break;
      } else if (config instanceof APMDataCollectionInfo) {
        APMDataCollectionInfo dInfo = (APMDataCollectionInfo) config;
        validateCollectorConfig = APMValidateCollectorConfig.builder()
                                      .baseUrl(dInfo.getBaseUrl())
                                      .url(dInfo.getValidationUrl())
                                      .options(dInfo.getOptions())
                                      .headers(dInfo.getHeaders())
                                      .build();
        break;
      } else if (config instanceof APMValidateCollectorConfig) {
        validateCollectorConfig = (APMValidateCollectorConfig) config;
        break;
      }
    }

    logger.info("Validation config for delegate task validation is {}", validateCollectorConfig);
    boolean validated = validateCollector(validateCollectorConfig);
    logger.info("Validated APM delegate task {}, result is {}", delegateTaskId, validated);
    return singletonList(
        DelegateConnectionResult.builder().criteria(getCriteria().get(0)).validated(validated).build());
  }
  public boolean validateCollector(APMValidateCollectorConfig config) {
    try {
      if (config.getHeaders() == null) {
        config.setHeaders(new HashMap<>());
      }
      if (config.getOptions() == null) {
        config.setOptions(new HashMap<>());
      }
      config.getHeaders().put("Accept", "application/json");
      final Call<Object> request =
          getAPMRestClient(config).validate(config.getUrl(), config.getHeaders(), config.getOptions());
      final Response<Object> response;

      response = request.execute();
      if (response.code() != 400) {
        return true;
      } else {
        return false;
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
