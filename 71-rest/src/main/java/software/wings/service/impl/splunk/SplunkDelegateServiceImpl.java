package software.wings.service.impl.splunk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.URL_STRING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.splunk.Args;
import com.splunk.Event;
import com.splunk.HttpException;
import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.SavedSearchCollection;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.beans.CVHistogram.CVHistogramBuilder;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpStatus;
import org.apache.xerces.impl.dv.util.Base64;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.SplunkConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of Splunk Delegate Service.
 * Created by rsingh on 6/30/17.
 */
@Singleton
@Slf4j
public class SplunkDelegateServiceImpl implements SplunkDelegateService {
  public static final String START_TIME = "Start Time";
  public static final String END_TIME = "End Time";
  private static final FastDateFormat rfc3339 =
      FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));
  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);
  private static final long BAR_DURATION_IN_HOURS = 6;

  private final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      encryptionService.decrypt(splunkConfig, encryptedDataDetails);
      logger.info("Validating splunk, url {}, for user {} ", splunkConfig.getSplunkUrl(), splunkConfig.getUsername());
      Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
      createSearchJob(splunkService, getQuery("*exception*", null, null, false),
          System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(5), System.currentTimeMillis());
      return true;
    } catch (HttpException e) {
      throw e;
    } catch (Exception e) {
      throw new DataCollectionException(e);
    }
  }

  private Job createSearchJob(Service splunkService, String query, long startTimeMillis, long endTimeMillis) {
    JobArgs jobargs = new JobArgs();
    jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

    jobargs.setEarliestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(startTimeMillis)));
    jobargs.setLatestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(endTimeMillis)));

    return splunkService.getJobs().create(query, jobargs);
  }

  private List<LogElement> fetchSearchResults(
      Job job, String basicQuery, String hostnameField, long logCollectionMinute) throws Exception {
    JobResultsArgs resultsArgs = new JobResultsArgs();
    resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);

    InputStream results = job.getResults(resultsArgs);
    ResultsReaderJson resultsReader = new ResultsReaderJson(results);
    List<LogElement> logElements = new ArrayList<>();

    Event event;
    while ((event = resultsReader.getNextEvent()) != null) {
      final LogElement splunkLogElement = new LogElement();
      splunkLogElement.setQuery(basicQuery);
      splunkLogElement.setClusterLabel(event.get("cluster_label"));
      splunkLogElement.setHost(event.get(hostnameField));
      splunkLogElement.setCount(Integer.parseInt(event.get("cluster_count")));
      splunkLogElement.setLogMessage(event.get("_raw"));
      // splunkLogElement.setTimeStamp(SPLUNK_DATE_FORMATER.parse(event.get("_time")).getTime());
      splunkLogElement.setTimeStamp(System.currentTimeMillis());
      splunkLogElement.setLogCollectionMinute(logCollectionMinute);
      logElements.add(splunkLogElement);
    }
    resultsReader.close();

    return logElements;
  }

  @Override
  public List<LogElement> getLogResults(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String basicQuery, String hostNameField, String host, long startTime, long endTime,
      ThirdPartyApiCallLog apiCallLog, int logCollectionMinute, boolean isAdvancedQuery) {
    Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
    String query = getQuery(basicQuery, hostNameField, host, isAdvancedQuery);
    addThirdPartyAPILogRequestFields(apiCallLog, splunkConfig.getSplunkUrl(), query, startTime, endTime);
    logger.info("triggering splunk query startTime: " + startTime + " endTime: " + endTime + " query: " + query
        + " url: " + splunkConfig.getSplunkUrl());

    try {
      Job job = createSearchJob(splunkService, query, startTime, endTime);
      List<LogElement> logElements = fetchSearchResults(job, basicQuery, hostNameField, logCollectionMinute);

      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_OK, createJSONBodyForThirdPartyAPILogs(logElements), FieldType.JSON);
      delegateLogService.save(splunkConfig.getAccountId(), apiCallLog);
      return logElements;
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(splunkConfig.getAccountId(), apiCallLog);
      throw new WingsException(ErrorCode.SPLUNK_CONFIGURATION_ERROR).addParam("reason", e);
    }
  }

  @Override
  public List<SplunkSavedSearch> getSavedSearches(
      SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
    Args queryArgs = new Args();
    queryArgs.put("app", "search");
    SavedSearchCollection savedSearchCollection = splunkService.getSavedSearches(queryArgs);
    return savedSearchCollection.values()
        .stream()
        .map(splunkSearch
            -> SplunkSavedSearch.builder().title(splunkSearch.getTitle()).searchQuery(splunkSearch.getSearch()).build())
        .collect(Collectors.toList());
  }

  @Override
  public CVHistogram getHistogram(
      SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails, String query) {
    Preconditions.checkNotNull(splunkConfig, "splunkConfig can not be null");
    Preconditions.checkNotNull(query, "query can not be null");
    Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
    CVHistogramBuilder cvHistogram =
        CVHistogram.builder().query(query).intervalMs(Duration.ofHours(BAR_DURATION_IN_HOURS).toMillis());

    try {
      ResultsReaderJson resultsReaderJson = executeSearch(splunkService, getHistogramQuery(query),
          Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli(), Instant.now().toEpochMilli());

      Event event;
      while ((event = resultsReaderJson.getNextEvent()) != null) {
        String time = event.get("_time");
        long timestamp = SPLUNK_DATE_FORMATER.parse(time).getTime();
        String count = event.get("count");
        cvHistogram.addBar(CVHistogram.Bar.builder().count(Long.parseLong(count)).timestamp(timestamp).build());
      }
    } catch (IOException | ParseException e) {
      throw new IllegalStateException(e);
    }
    return cvHistogram.build();
  }

  @NotNull
  private String getHistogramQuery(String query) {
    return "search " + query + " | timechart count span=" + BAR_DURATION_IN_HOURS + "h | table _time, count";
  }

  @Override
  public SplunkSampleResponse getSamples(
      SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails, String query) {
    Preconditions.checkNotNull(splunkConfig, "splunkConfig can not be null");
    Preconditions.checkNotNull(query, "query can not be null");

    Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
    ResultsReaderJson resultsReaderJson;
    List<String> results = new ArrayList<>();
    Map<String, String> sample = new HashMap<>();
    try {
      resultsReaderJson = executeSearch(splunkService, "search " + query + " | head 10",
          Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli(), Instant.now().toEpochMilli());
      Event event;
      while ((event = resultsReaderJson.getNextEvent()) != null) {
        if (sample.isEmpty()) {
          sample = event;
        }
        results.add(event.get("_raw"));
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    // Fields starting with underscore are internal fields.
    // https://docs.splunk.com/Splexicon:Internalfield
    Map<String, String> withoutInternalFields =
        sample.entrySet()
            .stream()
            .filter(entry -> entry.getKey().charAt(0) != '_')
            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));

    return SplunkSampleResponse.builder().sample(withoutInternalFields).rawSampleLogs(results).build();
  }

  private ResultsReaderJson executeSearch(Service service, String query, long startTimeMs, long endTimeMs)
      throws IOException {
    Job job = createSearchJob(service, query, startTimeMs, endTimeMs);
    JobResultsArgs resultsArgs = new JobResultsArgs();
    resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);

    InputStream results = job.getResults(resultsArgs);
    return new ResultsReaderJson(results);
  }

  private void addThirdPartyAPILogRequestFields(
      ThirdPartyApiCallLog apiCallLog, String splunkUrl, String query, long startTime, long endTime) {
    apiCallLog.setTitle("Fetch request to " + splunkUrl);
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(URL_STRING).value(splunkUrl).type(FieldType.URL).build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("Query").value(query).type(FieldType.TEXT).build());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(START_TIME)
                                     .value(getDateFormatTime(startTime))
                                     .type(FieldType.TIMESTAMP)
                                     .build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(END_TIME)
                                     .value(getDateFormatTime(endTime))
                                     .type(FieldType.TIMESTAMP)
                                     .build());
  }

  private String createJSONBodyForThirdPartyAPILogs(List<LogElement> logElements) {
    List<String> logs = logElements.stream().map(LogElement::getLogMessage).collect(Collectors.toList());
    SplunkJSONResponse splunkResponse = new SplunkJSONResponse(logs);
    Gson gson = new Gson();
    return gson.toJson(splunkResponse);
  }

  @VisibleForTesting
  Service initSplunkServiceWithToken(SplunkConfig splunkConfig) {
    final ServiceArgs loginArgs = new ServiceArgs();
    loginArgs.setUsername(splunkConfig.getUsername());
    loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));
    URI uri;
    try {
      uri = new URI(splunkConfig.getSplunkUrl().trim());
      final URL url = new URL(splunkConfig.getSplunkUrl().trim());
      loginArgs.setHost(url.getHost());
      loginArgs.setPort(url.getPort());

      loginArgs.setScheme(uri.getScheme());
      if (uri.getScheme().equals("https")) {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      }
      Service splunkService = new Service(loginArgs);

      splunkService.setConnectTimeout(HTTP_TIMEOUT);
      splunkService.setReadTimeout(HTTP_TIMEOUT);
      splunkService = Service.connect(loginArgs);
      return splunkService;
    } catch (Exception ex) {
      throw new WingsException("Unable to connect to server : " + ExceptionUtils.getMessage(ex));
    }
  }

  @VisibleForTesting
  Service initSplunkServiceWithBasicAuth(SplunkConfig splunkConfig) {
    URI uri;
    try {
      uri = new URI(splunkConfig.getSplunkUrl().trim());
      final URL url = new URL(splunkConfig.getSplunkUrl().trim());

      Service splunkService = new Service(url.getHost(), url.getPort(), uri.getScheme());
      String credentials = splunkConfig.getUsername() + ":" + splunkConfig.getPassword();
      String basicAuthHeader = Base64.encode(credentials.getBytes());
      splunkService.setToken("Basic " + basicAuthHeader);

      if (uri.getScheme().equals("https")) {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      }

      splunkService.setConnectTimeout(HTTP_TIMEOUT);
      splunkService.setReadTimeout(HTTP_TIMEOUT);

      splunkService.getApplications();

      return splunkService;
    } catch (Exception ex2) {
      throw new WingsException("Unable to connect to server : " + ExceptionUtils.getMessage(ex2));
    }
  }

  @Override
  public Service initSplunkService(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(splunkConfig, encryptedDataDetails);

    try {
      return initSplunkServiceWithToken(splunkConfig);

    } catch (Exception ex1) {
      logger.error("Token based splunk connection failed. Trying basic auth", ex1);
      return initSplunkServiceWithBasicAuth(splunkConfig);
    }
  }

  private String getQuery(String query, String hostNameField, String host, boolean isAdvancedQuery) {
    String searchQuery = isAdvancedQuery ? query + " " : "search " + query + " ";
    if (!(isEmpty(host) || DUMMY_HOST_NAME.equals(host))) {
      searchQuery += hostNameField + " = " + host;
    }
    searchQuery += " | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t"
        + "| table _time, _raw,cluster_label, " + hostNameField + " | "
        + "stats latest(_raw) as _raw count as cluster_count by _time,cluster_label," + hostNameField;

    return searchQuery;
  }

  private String getDateFormatTime(long time) {
    return rfc3339.format(new Date(time));
  }

  private static class SplunkJSONResponse {
    private List<String> logs;
    private long logCount;
    SplunkJSONResponse(List<String> logs) {
      this.logs = logs;
      this.logCount = logs.size();
    }
  }
}
