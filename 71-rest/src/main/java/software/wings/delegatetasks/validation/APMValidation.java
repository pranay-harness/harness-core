package software.wings.delegatetasks.validation;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import io.harness.beans.DelegateTask;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DatadogConfig;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.apm.APMDataCollectionInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class APMValidation extends AbstractSecretManagerValidation {
  public APMValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<String> getCriteria() {
    return singletonList(Arrays.stream(getParameters())
                             .filter(o
                                 -> o instanceof DatadogConfig || o instanceof APMVerificationConfig
                                     || o instanceof APMValidateCollectorConfig || o instanceof APMDataCollectionInfo
                                     || o instanceof CustomLogDataCollectionInfo || o instanceof BugsnagConfig)
                             .map(obj -> {
                               if (obj instanceof DatadogConfig) {
                                 DatadogConfig config = (DatadogConfig) obj;
                                 return config.getUrl() + DatadogConfig.validationUrl;
                               } else if (obj instanceof BugsnagConfig) {
                                 BugsnagConfig config = (BugsnagConfig) obj;
                                 return config.getUrl() + BugsnagConfig.validationUrl;
                               } else if (obj instanceof APMVerificationConfig) {
                                 APMVerificationConfig config = (APMVerificationConfig) obj;
                                 return config.getUrl() + config.getValidationUrl();
                               } else if (obj instanceof APMDataCollectionInfo) {
                                 APMDataCollectionInfo dInfo = (APMDataCollectionInfo) obj;
                                 return dInfo.getBaseUrl() + dInfo.getValidationUrl();
                               } else if (obj instanceof CustomLogDataCollectionInfo) {
                                 CustomLogDataCollectionInfo dInfo = (CustomLogDataCollectionInfo) obj;
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
        if (((APMDataCollectionInfo) parmeter).getEncryptedDataDetails() != null
            && ((APMDataCollectionInfo) parmeter).getEncryptedDataDetails().size() > 0) {
          return ((APMDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
        }
      } else if (parmeter instanceof CustomLogDataCollectionInfo) {
        if (((CustomLogDataCollectionInfo) parmeter).getEncryptedDataDetails() != null
            && ((CustomLogDataCollectionInfo) parmeter).getEncryptedDataDetails().size() > 0) {
          return ((CustomLogDataCollectionInfo) parmeter).getEncryptedDataDetails().get(0).getEncryptionConfig();
        }
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
      } else if (config instanceof BugsnagConfig) {
        BugsnagConfig bugsnag = (BugsnagConfig) config;
        validateCollectorConfig = bugsnag.createAPMValidateCollectorConfig();
        break;
      } else if (config instanceof APMVerificationConfig) {
        APMVerificationConfig apmConfig = (APMVerificationConfig) config;
        validateCollectorConfig = APMValidateCollectorConfig.builder().baseUrl(apmConfig.getUrl()).build();
        break;
      } else if (config instanceof APMDataCollectionInfo) {
        APMDataCollectionInfo dInfo = (APMDataCollectionInfo) config;
        validateCollectorConfig = APMValidateCollectorConfig.builder().baseUrl(dInfo.getBaseUrl()).build();
        break;
      } else if (config instanceof CustomLogDataCollectionInfo) {
        CustomLogDataCollectionInfo dInfo = (CustomLogDataCollectionInfo) config;
        validateCollectorConfig = APMValidateCollectorConfig.builder().baseUrl(dInfo.getBaseUrl()).build();
        break;
      } else if (config instanceof APMValidateCollectorConfig) {
        validateCollectorConfig = (APMValidateCollectorConfig) config;
        validateCollectorConfig.setUrl(null);
        validateCollectorConfig.setBody(null);
        validateCollectorConfig.setOptions(null);
        validateCollectorConfig.setHeaders(null);
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
      Call<Object> request =
          getAPMRestClient(config).validate(config.getUrl(), config.getHeaders(), config.getOptions());
      if (isNotEmpty(config.getBody())) {
        request = getAPMRestClient(config).validatePost(
            config.getUrl(), config.getHeaders(), config.getOptions(), new JSONObject(config.getBody()).toMap());
      }
      final Response<Object> response;

      response = request.execute();
      if (response.code() != 400) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      return false;
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
