package software.wings.delegatetasks.cv;

import static software.wings.common.VerificationConstants.URL_STRING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;

import com.splunk.Event;
import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpStatus;
import org.apache.xerces.impl.dv.util.Base64;
import software.wings.beans.SplunkConfig;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.splunk.SplunkDataCollectionInfoV2;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SplunkDataCollector implements LogDataCollector<SplunkDataCollectionInfoV2> {
  private static final String START_TIME = "Start Time";
  private static final String END_TIME = "End Time";
  private static final FastDateFormat rfc3339 =
      FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));
  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);

  private SplunkDataCollectionInfoV2 splunkDataCollectionInfo;
  private DataCollectionExecutionContext dataCollectionExecutionContext;

  @Override
  public void init(DataCollectionExecutionContext dataCollectionExecutionContext,
      SplunkDataCollectionInfoV2 splunkDataCollectionInfo) {
    this.splunkDataCollectionInfo = splunkDataCollectionInfo;
    this.dataCollectionExecutionContext = dataCollectionExecutionContext;
    initSplunkService(splunkDataCollectionInfo.getSplunkConfig());
  }

  @Override
  public int getHostBatchSize() {
    return 1;
  }
  @Override
  public List<LogElement> fetchLogs(List<String> hostBatch) throws DataCollectionException {
    Preconditions.checkArgument(hostBatch.size() == 1);
    return fetchLogs(Optional.of(hostBatch.get(0)));
  }

  @Override
  public List<LogElement> fetchLogs() throws DataCollectionException {
    return fetchLogs(Optional.empty());
  }

  public List<LogElement> fetchLogs(Optional<String> host) {
    Service splunkService = initSplunkService(splunkDataCollectionInfo.getSplunkConfig());
    String splunkQuery = getSplunkQuery(splunkDataCollectionInfo.getQuery(),
        splunkDataCollectionInfo.getHostnameField(), host, splunkDataCollectionInfo.isAdvancedQuery());
    ThirdPartyApiCallLog apiCallLog = dataCollectionExecutionContext.createApiCallLog();
    addThirdPartyAPILogRequestFields(apiCallLog, splunkDataCollectionInfo.getSplunkConfig().getSplunkUrl(), splunkQuery,
        splunkDataCollectionInfo.getStartTime(), splunkDataCollectionInfo.getEndTime());
    logger.info("triggering splunk query startTime: " + splunkDataCollectionInfo.getStartTime()
        + " endTime: " + splunkDataCollectionInfo.getEndTime() + " query: " + splunkQuery
        + " url: " + splunkDataCollectionInfo.getSplunkConfig());

    try {
      Job job = createSearchJob(
          splunkService, splunkQuery, splunkDataCollectionInfo.getStartTime(), splunkDataCollectionInfo.getEndTime());
      List<LogElement> logElements =
          fetchSearchResults(job, splunkDataCollectionInfo.getQuery(), splunkDataCollectionInfo.getHostnameField());
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_OK, createJSONBodyForThirdPartyAPILogs(logElements), FieldType.JSON);
      dataCollectionExecutionContext.saveThirdPartyApiCallLog(apiCallLog);
      return logElements;
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      dataCollectionExecutionContext.saveThirdPartyApiCallLog(apiCallLog);
      throw new DataCollectionException(e);
    }
  }

  @VisibleForTesting
  Job createSearchJob(Service splunkService, String query, Instant startTime, Instant endTime) {
    JobArgs jobargs = new JobArgs();
    jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

    jobargs.setEarliestTime(String.valueOf(startTime.getEpochSecond()));
    jobargs.setLatestTime(String.valueOf(endTime.getEpochSecond()));

    return splunkService.getJobs().create(query, jobargs);
  }

  private List<LogElement> fetchSearchResults(Job job, String query, String hostnameField) throws Exception {
    JobResultsArgs resultsArgs = new JobResultsArgs();
    resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);

    InputStream results = job.getResults(resultsArgs);
    ResultsReaderJson resultsReader = new ResultsReaderJson(results);
    List<LogElement> logElements = new ArrayList<>();

    Event event;
    while ((event = resultsReader.getNextEvent()) != null) {
      final LogElement splunkLogElement = new LogElement();
      splunkLogElement.setQuery(query);
      splunkLogElement.setClusterLabel(event.get("cluster_label"));
      splunkLogElement.setHost(event.get(hostnameField));
      splunkLogElement.setCount(Integer.parseInt(event.get("cluster_count")));
      splunkLogElement.setLogMessage(event.get("_raw"));
      splunkLogElement.setTimeStamp(System.currentTimeMillis());
      splunkLogElement.setLogCollectionMinute(
          TimeUnit.MILLISECONDS.toMinutes(splunkDataCollectionInfo.getStartTime().toEpochMilli()));
      logElements.add(splunkLogElement);
    }
    resultsReader.close();

    return logElements;
  }

  private void addThirdPartyAPILogRequestFields(
      ThirdPartyApiCallLog apiCallLog, String splunkUrl, String splunkQuery, Instant startTime, Instant endTime) {
    apiCallLog.setTitle("Fetch request to " + splunkUrl);
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name(URL_STRING).value(splunkUrl).type(FieldType.URL).build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("Query").value(splunkQuery).type(FieldType.TEXT).build());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(START_TIME)
                                     .value(getDateFormatTime(startTime.toEpochMilli()))
                                     .type(FieldType.TIMESTAMP)
                                     .build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(END_TIME)
                                     .value(getDateFormatTime(endTime.toEpochMilli()))
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
      throw new DataCollectionException("Unable to connect to server : " + ExceptionUtils.getMessage(ex));
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
      throw new DataCollectionException("Unable to connect to server : " + ExceptionUtils.getMessage(ex2));
    }
  }

  private Service initSplunkService(SplunkConfig splunkConfig) {
    try {
      return initSplunkServiceWithToken(splunkConfig);
    } catch (Exception ex1) {
      logger.error("Token based splunk connection failed. Trying basic auth", ex1);
      return initSplunkServiceWithBasicAuth(splunkConfig);
    }
  }

  private String getSplunkQuery(String query, String hostNameField, Optional<String> host, boolean isAdvancedQuery) {
    String searchQuery = isAdvancedQuery ? query + " " : "search " + query + " ";
    if (host.isPresent()) {
      searchQuery += hostNameField + " = " + host.get();
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
