package software.wings.service.impl.elk;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ElkConfig;
import software.wings.beans.KibanaConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.elk.ElkRestClient;
import software.wings.helpers.ext.elk.KibanaRestClient;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
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
  @Override
  public void validateConfig(ElkConfig elkConfig) {
    try {
      if (!StringUtils.isBlank(elkConfig.getUsername()) && elkConfig.getPassword() == null) {
        throw new WingsException("User name is given but password is empty");
      }

      if (StringUtils.isBlank(elkConfig.getUsername()) && elkConfig.getPassword() != null) {
        throw new WingsException("User name is empty but password is given");
      }

      getIndices(elkConfig);
    } catch (Throwable t) {
      throw new WingsException(t.getMessage());
    }
  }

  @Override
  public Object search(ElkConfig elkConfig, ElkLogFetchRequest logFetchRequest) throws IOException {
    final Call<Object> request = SettingVariableTypes.valueOf(elkConfig.getType()) == SettingVariableTypes.KIBANA
        ? getKibanaRestClient(elkConfig).getLogSample(
              String.format(KibanaRestClient.searchPathPattern, logFetchRequest.getIndices(), 10000),
              KibanaRestClient.searchMethod, logFetchRequest.toElasticSearchJsonObject())
        : getElkRestClient(elkConfig).search(logFetchRequest.getIndices(), logFetchRequest.toElasticSearchJsonObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }

    throw new WingsException(response.errorBody().string());
  }

  @Override
  public Map<String, ElkIndexTemplate> getIndices(ElkConfig elkConfig) throws IOException {
    final Call<Map<String, Map<String, Object>>> request =
        SettingVariableTypes.valueOf(elkConfig.getType()) == SettingVariableTypes.KIBANA
        ? getKibanaRestClient(elkConfig).template()
        : getElkRestClient(elkConfig).template();
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
            indexTemplate.setName((String) indexEntry.getValue().get("template"));
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
  public Object getLogSample(ElkConfig elkConfig, String index) throws IOException {
    final Call<Object> request = SettingVariableTypes.valueOf(elkConfig.getType()) == SettingVariableTypes.KIBANA
        ? getKibanaRestClient(elkConfig).getLogSample(String.format(KibanaRestClient.searchPathPattern, index, 1),
              KibanaRestClient.searchMethod, ElkLogFetchRequest.lastInsertedRecordObject())
        : getElkRestClient(elkConfig).getLogSample(index, ElkLogFetchRequest.lastInsertedRecordObject());
    final Response<Object> response = request.execute();
    if (response.isSuccessful()) {
      return response.body();
    }
    throw new WingsException(response.errorBody().string());
  }

  private ElkRestClient getElkRestClient(final ElkConfig elkConfig) {
    return createRetrofit(elkConfig).create(ElkRestClient.class);
  }

  private KibanaRestClient getKibanaRestClient(final ElkConfig elkConfig) {
    return createRetrofit(elkConfig).create(KibanaRestClient.class);
  }

  private Retrofit createRetrofit(ElkConfig elkConfig) {
    OkHttpClient.Builder httpClient =
        elkConfig.getElkUrl().startsWith("https") ? getUnsafeOkHttpClient() : new OkHttpClient.Builder();
    httpClient
        .addInterceptor(chain -> {
          Request original = chain.request();

          boolean shouldAuthenticate = !StringUtils.isBlank(elkConfig.getUsername()) && elkConfig.getPassword() != null;
          boolean isKibana = SettingVariableTypes.valueOf(elkConfig.getType()) == SettingVariableTypes.KIBANA;
          Request.Builder builder = shouldAuthenticate
              ? original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", getHeaderWithCredentials(elkConfig))
              : original.newBuilder().header("Accept", "application/json").header("Content-Type", "application/json");

          if (isKibana) {
            builder.addHeader("kbn-version", ((KibanaConfig) elkConfig).getKibanaVersion());
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

  private String getHeaderWithCredentials(ElkConfig elkConfig) {
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
  throw new RuntimeException(e);
}
}
}
