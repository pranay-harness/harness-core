package software.wings.delegatetasks.cv;

import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.network.Http.getOkHttpClientBuilderWithReadtimeOut;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import io.harness.network.Http;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ElkConfig;
import software.wings.helpers.ext.elk.ElkRestClient;
import software.wings.helpers.ext.elk.KibanaRestClient;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.elk.ElkDataCollectionInfoV2;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
@Slf4j
public class ElkDataCollector implements LogDataCollector<ElkDataCollectionInfoV2> {
  private ElkDataCollectionInfoV2 dataCollectionInfo;
  private DataCollectionExecutionContext context;
  @Override
  public void init(DataCollectionExecutionContext dataCollectionExecutionContext,
      ElkDataCollectionInfoV2 dataCollectionInfo) throws DataCollectionException {
    this.dataCollectionInfo = dataCollectionInfo;
    this.context = dataCollectionExecutionContext;
  }

  @Override
  public int getHostBatchSize() {
    return 1;
  }

  @Override
  public List<LogElement> fetchLogs(List<String> hostBatch) throws DataCollectionException {
    Preconditions.checkArgument(hostBatch.size() == 1);
    ElkLogFetchRequest elkLogFetchRequest = buildFetchRequest(new HashSet<>(hostBatch));
    Object response = search(dataCollectionInfo.getElkConfig(), elkLogFetchRequest, 1000);
    return parseElkResponse(response, Optional.of(hostBatch.get(0)));
  }
  private ElkLogFetchRequest buildFetchRequest(Set<String> hosts) {
    return ElkLogFetchRequest.builder()
        .query(dataCollectionInfo.getQuery())
        .indices(dataCollectionInfo.getIndices())
        .hostnameField(dataCollectionInfo.getHostnameField())
        .messageField(dataCollectionInfo.getMessageField())
        .timestampField(dataCollectionInfo.getTimestampField())
        .hosts(hosts)
        .startTime(dataCollectionInfo.getStartTime().toEpochMilli())
        .endTime(dataCollectionInfo.getEndTime().toEpochMilli())
        .queryType(dataCollectionInfo.getQueryType() == null ? ElkQueryType.TERM : dataCollectionInfo.getQueryType())
        .build();
  }
  @Override
  public List<LogElement> fetchLogs() throws DataCollectionException {
    ElkLogFetchRequest elkLogFetchRequest = buildFetchRequest(Collections.emptySet());
    Object response = search(dataCollectionInfo.getElkConfig(), elkLogFetchRequest, 1000);
    return parseElkResponse(response, Optional.empty());
  }

  public Object search(ElkConfig elkConfig, ElkLogFetchRequest logFetchRequest, int maxRecords) {
    final Call<Object> request = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER
        ? getKibanaRestClient(elkConfig).getLogSample(
              format(KibanaRestClient.searchPathPattern, logFetchRequest.getIndices(), 10000),
              KibanaRestClient.searchMethod, logFetchRequest.toElasticSearchJsonObject())
        : getElkRestClient(elkConfig).search(
              logFetchRequest.getIndices(), logFetchRequest.toElasticSearchJsonObject(), maxRecords);
    return context.executeRequest("Fetching logs from " + elkConfig.getElkUrl(), request);
  }

  private List<LogElement> parseElkResponse(Object searchResponse, Optional<String> hostname) {
    List<LogElement> logElements = new ArrayList<>();
    JSONObject responseObject = new JSONObject(JsonUtils.asJson(searchResponse));
    JSONObject hits = responseObject.getJSONObject("hits");
    if (hits == null) {
      return logElements;
    }

    SimpleDateFormat timeFormatter = new SimpleDateFormat(dataCollectionInfo.getTimestampFieldFormat());
    JSONArray logHits = hits.getJSONArray("hits");

    for (int i = 0; i < logHits.length(); i++) {
      JSONObject source = logHits.optJSONObject(i).getJSONObject("_source");
      if (source == null) {
        continue;
      }

      final String host = parseAndGetValue(source, dataCollectionInfo.getHostnameField());

      // if this elkResponse doesn't belong to this host, ignore it.
      // We ignore case because we don't know if elasticsearch might just lowercase everything in the index.
      if (hostname.isPresent() && !hostname.get().trim().equalsIgnoreCase(host.trim())) {
        continue;
      }

      final String logMessage = parseAndGetValue(source, dataCollectionInfo.getMessageField());

      final String timeStamp = parseAndGetValue(source, dataCollectionInfo.getTimestampField());
      long timeStampValue;
      try {
        timeStampValue = timeFormatter.parse(timeStamp).getTime();
      } catch (ParseException pe) {
        throw new DataCollectionException(pe);
      }

      if (timeStampValue < dataCollectionInfo.getStartTime().toEpochMilli()
          || timeStampValue > dataCollectionInfo.getEndTime().toEpochMilli()) {
        logger.info("received response outside the time range");
        continue;
      }

      final LogElement elkLogElement = new LogElement();
      elkLogElement.setQuery(dataCollectionInfo.getQuery());
      elkLogElement.setClusterLabel(String.valueOf(i));
      elkLogElement.setHost(host);
      elkLogElement.setCount(1);
      elkLogElement.setLogMessage(logMessage);
      elkLogElement.setTimeStamp(timeStampValue);
      elkLogElement.setLogCollectionMinute(TimeUnit.MILLISECONDS.toMinutes(timeStampValue));
      logElements.add(elkLogElement);
    }

    return logElements;
  }

  private String parseAndGetValue(JSONObject source, String field) {
    Object messageObject = source;
    String[] messagePaths = field.split("\\.");
    for (int j = 0; j < messagePaths.length; ++j) {
      if (messageObject instanceof JSONObject) {
        messageObject = ((JSONObject) messageObject).get(messagePaths[j]);
      } else if (messageObject instanceof JSONArray) {
        messageObject = ((JSONArray) messageObject).get(Integer.parseInt(messagePaths[j]));
      }
    }
    if (messageObject instanceof String) {
      return (String) messageObject;
    }
    throw new DataCollectionException("Unable to parse JSON response " + source.toString() + " and field " + field);
  }
  @VisibleForTesting
  ElkRestClient getElkRestClient(final ElkConfig elkConfig) {
    return createRetrofit(elkConfig).create(ElkRestClient.class);
  }

  @VisibleForTesting
  KibanaRestClient getKibanaRestClient(final ElkConfig elkConfig) {
    return createRetrofit(elkConfig).create(KibanaRestClient.class);
  }

  @VisibleForTesting
  Retrofit createRetrofit(ElkConfig elkConfig) {
    OkHttpClient.Builder httpClient = elkConfig.getElkUrl().startsWith("https")
        ? getUnsafeOkHttpClient().readTimeout(60, TimeUnit.SECONDS)
        : getOkHttpClientBuilderWithReadtimeOut(60, TimeUnit.SECONDS);
    httpClient
        .addInterceptor(chain -> {
          Request original = chain.request();

          boolean shouldAuthenticate = isNotBlank(elkConfig.getUsername()) && elkConfig.getPassword() != null;
          boolean isKibana = elkConfig.getElkConnector() == ElkConnector.KIBANA_SERVER;
          Request.Builder builder;
          if (shouldAuthenticate) {
            boolean usePassword = elkConfig.getValidationType() == ElkValidationType.PASSWORD;
            builder = usePassword ? original.newBuilder()
                                        .header("Accept", "application/json")
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", getHeaderWithCredentials(elkConfig))
                                  : original.newBuilder()
                                        .header("Accept", "application/json")
                                        .header("Content-Type", "application/json")
                                        .header(elkConfig.getUsername(), getAPIToken(elkConfig));
          } else {
            builder =
                original.newBuilder().header("Accept", "application/json").header("Content-Type", "application/json");
          }

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

  private String getHeaderWithCredentials(ElkConfig elkConfig) {
    return "Basic "
        + Base64.encodeBase64String(format("%s:%s", elkConfig.getUsername(), new String(elkConfig.getPassword()))
                                        .getBytes(StandardCharsets.UTF_8));
  }

  private String getAPIToken(ElkConfig elkConfig) {
    return String.valueOf(elkConfig.getPassword());
  }

  private static OkHttpClient.Builder getUnsafeOkHttpClient() {
    try {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager(){
          @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType){
              // all trust manager so no need to check
          }

          @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType){
              // all trust manager so no need to check
          }

          @Override public java.security.cert.X509Certificate[] getAcceptedIssuers(){return new X509Certificate[] {};
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
  throw new DataCollectionException(e);
}
}
}
