package software.wings.service.impl.splunk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.Constants.URL_STRING;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.splunk.Event;
import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import software.wings.beans.SplunkConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of Splunk Delegate Service.
 * Created by rsingh on 6/30/17.
 */
@Singleton
@Slf4j
public class SplunkDelegateServiceImpl implements SplunkDelegateService {
  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);
  private final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      encryptionService.decrypt(splunkConfig, encryptedDataDetails);
      logger.info("Validating splunk, url {}, for user {} ", splunkConfig.getSplunkUrl(), splunkConfig.getUsername());
      Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
      createSearchJob(splunkService, getQuery("*exception*", null, null),
          System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(5), System.currentTimeMillis());
      return true;
    } catch (Exception exception) {
      throw new WingsException("Error connecting to Splunk " + ExceptionUtils.getMessage(exception), exception);
    }
  }

  private Job createSearchJob(Service splunkService, String query, long startTimeMillis, long endTimeMillis) {
    JobArgs jobargs = new JobArgs();
    jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

    jobargs.setEarliestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(startTimeMillis)));
    jobargs.setLatestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(endTimeMillis)));

    return splunkService.getJobs().create(query, jobargs);
  }

  private List<LogElement> fetchSearchResults(Job job, String basicQuery, String host, long logCollectionMinute)
      throws Exception {
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
      splunkLogElement.setHost(host);
      splunkLogElement.setCount(Integer.parseInt(event.get("cluster_count")));
      splunkLogElement.setLogMessage(event.get("_raw"));
      splunkLogElement.setTimeStamp(SPLUNK_DATE_FORMATER.parse(event.get("_time")).getTime());
      splunkLogElement.setLogCollectionMinute(logCollectionMinute);
      logElements.add(splunkLogElement);
    }
    resultsReader.close();

    return logElements;
  }

  @Override
  public List<LogElement> getLogResults(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String basicQuery, String hostNameField, String host, long startTime, long endTime,
      ThirdPartyApiCallLog apiCallLog, int logCollectionMinute) {
    Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
    String query = getQuery(basicQuery, hostNameField, host);

    apiCallLog.setTitle("Fetch request to " + splunkConfig.getSplunkUrl());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name(URL_STRING)
                                     .value(splunkConfig.getSplunkUrl())
                                     .type(FieldType.URL)
                                     .build());
    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("Query").value(query).type(FieldType.TEXT).build());
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

    logger.info("triggering splunk query startTime: " + startTime + " endTime: " + endTime + " query: " + query
        + " url: " + splunkConfig.getSplunkUrl());

    try {
      Job job = createSearchJob(splunkService, query, startTime, endTime);
      List<LogElement> logElements = fetchSearchResults(job, basicQuery, host, logCollectionMinute);

      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(
          HttpStatus.SC_OK, "splunk query done. Num of events: " + job.getEventCount(), FieldType.TEXT);
      delegateLogService.save(splunkConfig.getAccountId(), apiCallLog);

      return logElements;

    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(splunkConfig.getAccountId(), apiCallLog);
      throw new WingsException(e);
    }
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

  private String getQuery(String query, String hostNameField, String host) {
    String searchQuery = "search " + query + " ";
    if (!isEmpty(host)) {
      searchQuery += hostNameField + " = " + host;
    }
    searchQuery += " | bin _time span=1m | cluster t=0.9999 showcount=t labelonly=t"
        + "| table _time, _raw,cluster_label, host | "
        + "stats latest(_raw) as _raw count as cluster_count by _time,cluster_label,host";

    return searchQuery;
  }
}
