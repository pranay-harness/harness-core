package software.wings.service.impl.elk;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ElkConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.elk.ElkRestClient;
import software.wings.helpers.ext.elk.KibanaRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
public class ElkDelegateServiceImpl implements ElkDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(ElkDelegateServiceImpl.class);

  @Inject private EncryptionService encryptionService;

  @Override
  public boolean validateConfig(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      if (isNotBlank(elkConfig.getUsername()) && elkConfig.getPassword() == null) {
        throw new WingsException("User name is given but password is empty");
      }

      if (isBlank(elkConfig.getUsername()) && elkConfig.getPassword() != null) {
        throw new WingsException("User name is empty but password is given");
      }
      if (elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER) {
        validate(elkConfig, encryptedDataDetails);
      } else {
        getIndices(elkConfig, encryptedDataDetails);
      }
      return true;
    } catch (Throwable t) {
      throw new WingsException(t.getMessage(), t);
    }
  }

  @Override
  public Object search(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ElkLogFetchRequest logFetchRequest) throws IOException {
    final Call<Object> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig, encryptedDataDetails)
              .getLogSample(String.format(KibanaRestClient.searchPathPattern, logFetchRequest.getIndices(), 10000),
                  KibanaRestClient.searchMethod, logFetchRequest.toElasticSearchJsonObject())
        : getElkRestClient(elkConfig, encryptedDataDetails)
              .search(logFetchRequest.getIndices(), logFetchRequest.toElasticSearchJsonObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }

    throw new WingsException(response.errorBody().string());
  }

  @Override
  public Map<String, ElkIndexTemplate> getIndices(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    final Call<Map<String, Map<String, Object>>> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig, encryptedDataDetails).template()
        : getElkRestClient(elkConfig, encryptedDataDetails).template();
    final Response<Map<String, Map<String, Object>>> response = request.execute();

    if (!response.isSuccessful()) {
      throw new WingsException(response.errorBody().string());
    }

    final Map<String, ElkIndexTemplate> rv = new HashMap<>();
    for (Entry<String, Map<String, Object>> indexEntry : response.body().entrySet()) {
      if (!indexEntry.getKey().startsWith(".")) {
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
    return rv;
  }

  @Override
  public Object getLogSample(ElkConfig elkConfig, String index, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    final Call<Object> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig, encryptedDataDetails)
              .getLogSample(String.format(KibanaRestClient.searchPathPattern, index, 1), KibanaRestClient.searchMethod,
                  ElkLogFetchRequest.lastInsertedRecordObject())
        : getElkRestClient(elkConfig, encryptedDataDetails)
              .getLogSample(index, ElkLogFetchRequest.lastInsertedRecordObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }
    throw new WingsException(response.errorBody().string());
  }

  private Object validate(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    if (elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER) {
      final Call<Object> request = getKibanaRestClient(elkConfig, encryptedDataDetails).validate();
      final Response<Object> response = request.execute();
      if (response.isSuccessful()) {
        return response.body();
      }
      throw new WingsException(response.errorBody().string());
    }

    throw new WingsException("Validate call not supported for " + elkConfig.getElkConnector());
  }

  @Override
  public String getVersion(ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException {
    if (elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER) {
      try {
        final Call<Object> request = getKibanaRestClient(elkConfig, encryptedDataDetails).version();
        final Response<Object> response = request.execute();
        return response.headers().get("kbn-version");
      } catch (Exception ex) {
        logger.warn("Unable to get Kibana version", ex);
        throw new WingsException("Unable to get version. Check url : " + ex.getMessage(), ex);
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
    OkHttpClient.Builder httpClient =
        elkConfig.getElkUrl().startsWith("https") ? getUnsafeOkHttpClient() : new OkHttpClient.Builder();
    httpClient
        .addInterceptor(chain -> {
          Request original = chain.request();

          boolean shouldAuthenticate = isNotBlank(elkConfig.getUsername()) && elkConfig.getPassword() != null;
          boolean isKibana = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER;
          Request.Builder builder = shouldAuthenticate
              ? original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", getHeaderWithCredentials(elkConfig, encryptedDataDetails))
              : original.newBuilder().header("Accept", "application/json").header("Content-Type", "application/json");

          if (isKibana) {
            builder.addHeader("kbn-version", elkConfig.getKibanaVersion());
          }

          Request request = builder.method(original.method(), original.body()).build();

          return chain.proceed(request);
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS);

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
        + Base64.encodeBase64String(String.format("%s:%s", elkConfig.getUsername(), new String(elkConfig.getPassword()))
                                        .getBytes(StandardCharsets.UTF_8));
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

OkHttpClient.Builder builder = new OkHttpClient.Builder();
builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
builder.hostnameVerifier((hostname, session) -> true);

return builder;
}
catch (Exception e) {
  throw new WingsException("Unexpected error : " + e.getMessage(), e);
}
}
}
