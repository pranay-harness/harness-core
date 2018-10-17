package software.wings.service.impl.splunk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.common.Constants.URL_STRING;

import com.google.inject.Inject;

import com.splunk.Event;
import com.splunk.HttpService;
import com.splunk.Job;
import com.splunk.JobArgs;
import com.splunk.JobResultsArgs;
import com.splunk.ResultsReaderJson;
import com.splunk.SSLSecurityProtocol;
import com.splunk.Service;
import com.splunk.ServiceArgs;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SplunkConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of Splunk Delegate Service.
 * Created by rsingh on 6/30/17.
 */
public class SplunkDelegateServiceImpl implements SplunkDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(SplunkDelegateServiceImpl.class);

  private static final int HTTP_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(25);
  private final SimpleDateFormat SPLUNK_DATE_FORMATER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public boolean validateConfig(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      encryptionService.decrypt(splunkConfig, encryptedDataDetails);
      logger.info("Validating splunk, url {}, for user {} ", splunkConfig.getSplunkUrl(), splunkConfig.getUsername());
      final ServiceArgs loginArgs = new ServiceArgs();
      loginArgs.setUsername(splunkConfig.getUsername());
      loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));

      final URL url = new URL(splunkConfig.getSplunkUrl());
      loginArgs.setHost(url.getHost());
      loginArgs.setPort(url.getPort());
      loginArgs.setScheme(url.toURI().getScheme());

      if (url.toURI().getScheme().equals("https")) {
        HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
      }

      Service service = new Service(loginArgs);
      service.setConnectTimeout(HTTP_TIMEOUT);
      service.setReadTimeout(HTTP_TIMEOUT);

      Service.connect(loginArgs);
      return true;
    } catch (MalformedURLException exception) {
      throw new WingsException(splunkConfig.getSplunkUrl() + " is not a valid url", exception);
    } catch (Exception exception) {
      throw new WingsException("Error connecting to Splunk " + Misc.getMessage(exception), exception);
    }
  }

  @Override
  public List<LogElement> getLogResults(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String basicQuery, String hostNameField, String host, long startTime, long endTime,
      ThirdPartyApiCallLog apiCallLog) {
    Service splunkService = initSplunkService(splunkConfig, encryptedDataDetails);
    String query = getQuery(basicQuery, hostNameField, host);
    JobArgs jobargs = new JobArgs();
    jobargs.setExecutionMode(JobArgs.ExecutionMode.BLOCKING);

    jobargs.setEarliestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(startTime)));
    jobargs.setLatestTime(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(endTime)));

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
    Job job = splunkService.getJobs().create(query, jobargs);

    JobResultsArgs resultsArgs = new JobResultsArgs();
    resultsArgs.setOutputMode(JobResultsArgs.OutputMode.JSON);

    InputStream results;
    try {
      results = job.getResults(resultsArgs);
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(splunkConfig.getAccountId(), apiCallLog);
      logger.warn("Failed to get job results from Splunk Server.");
      throw new WingsException(e.getMessage());
    }
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToResponse(200, "splunk query done. Num of events: " + job.getEventCount(), FieldType.TEXT);
    delegateLogService.save(splunkConfig.getAccountId(), apiCallLog);
    ResultsReaderJson resultsReader;
    try {
      resultsReader = new ResultsReaderJson(results);
    } catch (IOException e) {
      throw new WingsException("Unable to parse response to JSON : " + results);
    }
    List<LogElement> logElements = new ArrayList<>();
    try {
      Event event;
      while ((event = resultsReader.getNextEvent()) != null) {
        final LogElement splunkLogElement = new LogElement();
        splunkLogElement.setQuery(query);
        splunkLogElement.setClusterLabel(event.get("cluster_label"));
        splunkLogElement.setHost(host);
        splunkLogElement.setCount(Integer.parseInt(event.get("cluster_count")));
        splunkLogElement.setLogMessage(event.get("_raw"));
        splunkLogElement.setTimeStamp(SPLUNK_DATE_FORMATER.parse(event.get("_time")).getTime());
        splunkLogElement.setLogCollectionMinute((int) startTime);
        logElements.add(splunkLogElement);
      }
      resultsReader.close();
    } catch (IOException e) {
      throw new WingsException(e.getMessage());
    } catch (ParseException e) {
      throw new WingsException("Unable to parse cluster count to integer " + e.getMessage());
    }
    logger.info("for host {} got records {}", host, logElements.size());
    return logElements;
  }

  @Override
  public Service initSplunkService(SplunkConfig splunkConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(splunkConfig, encryptedDataDetails);
    final ServiceArgs loginArgs = new ServiceArgs();
    loginArgs.setUsername(splunkConfig.getUsername());
    loginArgs.setPassword(String.valueOf(splunkConfig.getPassword()));
    URI uri;
    try {
      uri = new URI(splunkConfig.getSplunkUrl().trim());
    } catch (Exception ex) {
      throw new WingsException("Invalid server URL " + splunkConfig.getSplunkUrl());
    }

    loginArgs.setHost(uri.getHost());
    loginArgs.setPort(uri.getPort());
    loginArgs.setScheme(uri.getScheme());
    if (uri.getScheme().equals("https")) {
      HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
    }
    Service splunkService = new Service(loginArgs);

    try {
      splunkService.setConnectTimeout(HTTP_TIMEOUT);
      splunkService.setReadTimeout(HTTP_TIMEOUT);
      splunkService = Service.connect(loginArgs);
    } catch (Exception ex) {
      throw new WingsException("Unable to connect to server : " + Misc.getMessage(ex));
    }
    return splunkService;
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
