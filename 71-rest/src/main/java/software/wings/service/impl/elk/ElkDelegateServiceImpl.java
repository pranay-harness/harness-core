package software.wings.service.impl.elk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.network.Http.getOkHttpClientBuilderWithReadtimeOut;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.serializer.JsonUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.Base;
import software.wings.beans.ElkConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.elk.ElkRestClient;
import software.wings.helpers.ext.elk.KibanaRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by rsingh on 8/01/17.
 */
@Singleton
public class ElkDelegateServiceImpl implements ElkDelegateService {
  public static final int MAX_RECORDS = 10000;

  private static final Logger logger = LoggerFactory.getLogger(ElkDelegateServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      if (isNotBlank(elkConfig.getUsername()) && isEmpty(elkConfig.getPassword())) {
        throw new WingsException("User name is given but password is empty");
      }

      if (isBlank(elkConfig.getUsername()) && isNotEmpty(elkConfig.getPassword())) {
        throw new WingsException("User name is empty but password is given");
      }
      getLogSample(elkConfig, "*", false, encryptedDataDetails);
      return true;
    } catch (Exception exception) {
      throw new WingsException(ExceptionUtils.getMessage(exception), exception);
    }
  }

  @Override
  public Object search(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ElkLogFetchRequest logFetchRequest, ThirdPartyApiCallLog apiCallLog, int maxRecords) throws IOException {
    if (apiCallLog == null) {
      apiCallLog = createApiCallLog(elkConfig.getAccountId(), Base.GLOBAL_APP_ID, null);
    }

    apiCallLog.setTitle("Fetching logs from " + elkConfig.getElkUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("connector")
                                     .value(elkConfig.getElkConnector().getName())
                                     .type(FieldType.TEXT)
                                     .build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("indices")
                                     .value(logFetchRequest.getIndices())
                                     .type(FieldType.TEXT)
                                     .build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("body")
                                     .value(JsonUtils.asJson(logFetchRequest.toElasticSearchJsonObject()))
                                     .type(FieldType.JSON)
                                     .build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("url").value(elkConfig.getElkUrl()).type(FieldType.URL).build());
    final Call<Object> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig, encryptedDataDetails)
              .getLogSample(format(KibanaRestClient.searchPathPattern, logFetchRequest.getIndices(), 10000),
                  KibanaRestClient.searchMethod, logFetchRequest.toElasticSearchJsonObject())
        : getElkRestClient(elkConfig, encryptedDataDetails)
              .search(logFetchRequest.getIndices(), logFetchRequest.toElasticSearchJsonObject(), maxRecords);
    final Response<Object> response;
    try {
      response = request.execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(elkConfig.getAccountId(), apiCallLog);
      throw new WingsException(e.getMessage());
    }
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
      delegateLogService.save(elkConfig.getAccountId(), apiCallLog);
      return response.body();
    }

    apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
    delegateLogService.save(elkConfig.getAccountId(), apiCallLog);
    throw new WingsException("Unable to get search response - " + response.errorBody().string());
  }

  @Override
  public Map<String, ElkIndexTemplate> getIndices(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    if (apiCallLog == null) {
      apiCallLog = createApiCallLog(elkConfig.getAccountId(), Base.GLOBAL_APP_ID, null);
    }
    apiCallLog.setTitle("Fetching indices from " + elkConfig.getElkUrl());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.setRequest(Lists.newArrayList(ThirdPartyApiCallField.builder()
                                                 .name("connector")
                                                 .value(elkConfig.getElkConnector().getName())
                                                 .type(FieldType.TEXT)
                                                 .build(),
        ThirdPartyApiCallField.builder()
            .name("url")
            .value(elkConfig.getElkUrl() + "/_template")
            .type(FieldType.URL)
            .build()));
    final Call<Map<String, Map<String, Object>>> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig, encryptedDataDetails).template()
        : getElkRestClient(elkConfig, encryptedDataDetails).template();
    final Response<Map<String, Map<String, Object>>> response = request.execute();

    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    if (!response.isSuccessful()) {
      apiCallLog.addFieldToResponse(response.code(), response.errorBody().string(), FieldType.TEXT);
      delegateLogService.save(elkConfig.getAccountId(), apiCallLog);
      throw new WingsException(response.errorBody().string());
    }

    apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);
    final Map<String, ElkIndexTemplate> rv = new HashMap<>();
    for (Entry<String, Map<String, Object>> indexEntry : response.body().entrySet()) {
      if (indexEntry.getKey().charAt(0) != '.') {
        JSONObject jsonObject = new JSONObject((Map) indexEntry.getValue().get("mappings"));

        for (String key : jsonObject.keySet()) {
          JSONObject outerObject = jsonObject.getJSONObject(key);
          if (outerObject.get("properties") != null) {
            ElkIndexTemplate indexTemplate = new ElkIndexTemplate();
            if (indexEntry.getValue().containsKey("index_patterns")) {
              // TODO picking only the first pattern. should pick all patterns
              indexTemplate.setName(((ArrayList<String>) indexEntry.getValue().get("index_patterns")).get(0));
            } else {
              indexTemplate.setName((String) indexEntry.getValue().get("template"));
            }
            JSONObject propertiesObject = outerObject.getJSONObject("properties");
            final Map<String, Object> propertiesMap = new HashMap<>();
            for (String property : propertiesObject.keySet()) {
              propertiesMap.put(property, propertiesObject.getJSONObject(property).toMap());
            }
            indexTemplate.setProperties(propertiesMap);
            rv.put(indexTemplate.getName(), indexTemplate);
          }
        }
      }
    }
    delegateLogService.save(elkConfig.getAccountId(), apiCallLog);
    return rv;
  }

  @Override
  public Object getLogSample(ElkConfig elkConfig, String index, boolean shouldSort,
      List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    final Call<Object> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig, encryptedDataDetails)
              .getLogSample(format(KibanaRestClient.searchPathPattern, index, 1), KibanaRestClient.searchMethod,
                  ElkLogFetchRequest.lastInsertedRecordObject(shouldSort))
        : getElkRestClient(elkConfig, encryptedDataDetails)
              .getLogSample(index, ElkLogFetchRequest.lastInsertedRecordObject(shouldSort));
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }
    throw new WingsException(response.errorBody().string());
  }

  @Override
  public String getVersion(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    if (elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER) {
      try {
        final Call<Object> request = getKibanaRestClient(elkConfig, encryptedDataDetails).version();
        final Response<Object> response = request.execute();
        return response.headers().get("kbn-version");
      } catch (Exception ex) {
        throw new WingsException("Unable to get version. Check url : " + ExceptionUtils.getMessage(ex), ex);
      }
    } else {
      throw new WingsException("Get version is supported only for the Kibana connector");
    }
  }

  private ElkRestClient getElkRestClient(final ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return createRetrofit(elkConfig, encryptedDataDetails).create(ElkRestClient.class);
  }

  private KibanaRestClient getKibanaRestClient(
      final ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    return createRetrofit(elkConfig, encryptedDataDetails).create(KibanaRestClient.class);
  }

  private Retrofit createRetrofit(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(elkConfig, encryptedDataDetails);
    OkHttpClient.Builder httpClient = elkConfig.getElkUrl().startsWith("https")
        ? getUnsafeOkHttpClient().readTimeout(60, TimeUnit.SECONDS)
        : getOkHttpClientBuilderWithReadtimeOut(60, TimeUnit.SECONDS);
    httpClient
        .addInterceptor(chain -> {
          Request original = chain.request();

          boolean shouldAuthenticate = isNotBlank(elkConfig.getUsername()) && elkConfig.getPassword() != null;
          boolean usePassword = elkConfig.getValidationType() == ElkValidationType.PASSWORD;
          boolean isKibana = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER;
          Request.Builder builder = shouldAuthenticate
              ? usePassword ? original.newBuilder()
                                  .header("Accept", "application/json")
                                  .header("Content-Type", "application/json")
                                  .header("Authorization", getHeaderWithCredentials(elkConfig, encryptedDataDetails))
                            : original.newBuilder()
                                  .header("Accept", "application/json")
                                  .header("Content-Type", "application/json")
                                  .header(elkConfig.getUsername(), getAPIToken(elkConfig, encryptedDataDetails))
              : original.newBuilder().header("Accept", "application/json").header("Content-Type", "application/json");

          if (isKibana) {
            builder.addHeader("kbn-version", elkConfig.getKibanaVersion());
          }

          Request request = builder.method(original.method(), original.body()).build();

          return chain.proceed(request);
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .proxy(Http.checkAndGetNonProxyIfApplicable(elkConfig.getElkUrl()));

    String baseUrl = elkConfig.getElkUrl();
    if (baseUrl.charAt(baseUrl.length() - 1) != '/') {
      baseUrl = baseUrl + "/";
    }

    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(JacksonConverterFactory.create())
        .client(httpClient.build())
        .build();
  }

  private String getHeaderWithCredentials(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(elkConfig, encryptedDataDetails);
    return "Basic "
        + Base64.encodeBase64String(format("%s:%s", elkConfig.getUsername(), new String(elkConfig.getPassword()))
                                        .getBytes(StandardCharsets.UTF_8));
  }

  private String getAPIToken(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(elkConfig, encryptedDataDetails);
    return String.valueOf(elkConfig.getPassword());
  }

  private static OkHttpClient.Builder getUnsafeOkHttpClient() {
    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager(){
          @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
              throws CertificateException{}

          @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
              throws CertificateException{}

                     @Override public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                         return new X509Certificate[] {};
    }
  }
};

// Install the all-trusting trust manager
final SSLContext sslContext = SSLContext.getInstance("SSL");
sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
// Create an ssl socket factory with our all-trusting manager
final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

OkHttpClient.Builder builder = getOkHttpClientBuilder();
builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
builder.hostnameVerifier((hostname, session) -> true);

return builder;
}
catch (Exception e) {
  throw new WingsException("Unexpected error : " + ExceptionUtils.getMessage(e), e);
}
}
}
