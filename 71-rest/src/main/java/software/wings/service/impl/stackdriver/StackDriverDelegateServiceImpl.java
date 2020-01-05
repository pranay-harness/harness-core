package software.wings.service.impl.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofSeconds;
import static software.wings.common.VerificationConstants.RATE_LIMIT_STATUS;
import static software.wings.common.VerificationConstants.STACKDRIVER_DEFAULT_HOST_NAME_FIELD;
import static software.wings.common.VerificationConstants.STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD;
import static software.wings.common.VerificationConstants.STACK_DRIVER_QUERY_SEPARATER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.LOADBALANCER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.POD_NAME;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.Region;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.model.ListLogEntriesRequest;
import com.google.api.services.logging.v2.model.ListLogEntriesResponse;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jayway.jsonpath.JsonPath;
import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.exception.WingsException;
import io.harness.expression.SecretString;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import software.wings.beans.GcpConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;

import java.io.IOException;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by Pranjal on 11/27/2018
 */
@Singleton
@Slf4j
public class StackDriverDelegateServiceImpl implements StackDriverDelegateService {
  private static final FastDateFormat rfc3339 =
      FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));

  @Inject private EncryptionService encryptionService;
  @Inject private GcpHelperService gcpHelperService;
  @Inject private DelegateLogService delegateLogService;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(GcpConfig gcpConfig,
      List<EncryptedDataDetail> encryptionDetails, StackDriverSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    Monitoring monitoring = gcpHelperService.getMonitoringService(gcpConfig, encryptionDetails, projectId);
    String projectResource = "projects/" + projectId;
    List<ListTimeSeriesResponse> responses = new ArrayList<>();
    long startTime = setupTestNodeData.getFromTime() * TimeUnit.SECONDS.toMillis(1);
    long endTime = setupTestNodeData.getToTime() * TimeUnit.SECONDS.toMillis(1);

    if (!isEmpty(setupTestNodeData.getPodMetrics())) {
      setupTestNodeData.getPodMetrics().forEach(metric -> {
        String podFilter = createFilter(POD_NAME, metric.getMetricName(), hostName);
        ListTimeSeriesResponse response = getTimeSeriesResponse(
            monitoring, projectResource, gcpConfig, podFilter, startTime, endTime, apiCallLog.copy());
        if (isNotEmpty(response)) {
          responses.add(response);
        }
      });
    }

    if (!isEmpty(setupTestNodeData.getLoadBalancerMetrics())) {
      setupTestNodeData.getLoadBalancerMetrics().forEach((ruleName, lbMetrics) -> lbMetrics.forEach(lbMetric -> {
        String lbFilter = createFilter(LOADBALANCER, lbMetric.getMetricName(), ruleName);
        ListTimeSeriesResponse response = getTimeSeriesResponse(
            monitoring, projectResource, gcpConfig, lbFilter, startTime, endTime, apiCallLog.copy());
        if (isNotEmpty(response)) {
          responses.add(response);
        }
      }));
    }

    if (!isEmpty(setupTestNodeData.getMetricDefinitions())) {
      setupTestNodeData.getMetricDefinitions().forEach(metricDefinition -> {
        String filter = metricDefinition.getFilter();
        ListTimeSeriesResponse response = getTimeSeriesResponse(
            monitoring, projectResource, gcpConfig, filter, startTime, endTime, apiCallLog.copy());
        if (isNotEmpty(response)) {
          responses.add(response);
        }
      });
    }

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(
            VerificationLoadResponse.builder().isLoadPresent(isNotEmpty(responses)).loadResponse(responses).build())
        .dataForNode(responses)
        .build();
  }

  @Override
  public List<String> listRegions(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    try {
      List<Region> regions = gcpHelperService.getGCEService(gcpConfig, encryptionDetails, projectId)
                                 .regions()
                                 .list(projectId)
                                 .execute()
                                 .getItems();
      if (isNotEmpty(regions)) {
        return regions.stream().map(Region::getName).collect(Collectors.toList());
      }
    } catch (Exception e) {
      throw new WingsException(ErrorCode.STACKDRIVER_ERROR).addParam("reason", getMessage(e));
    }
    return Collections.emptyList();
  }

  @Override
  public Map<String, String> listForwardingRules(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String region) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    try {
      List<ForwardingRule> forwardingRulesByRegion =
          gcpHelperService.getGCEService(gcpConfig, encryptionDetails, projectId)
              .forwardingRules()
              .list(projectId, region)
              .execute()
              .getItems();
      if (isNotEmpty(forwardingRulesByRegion)) {
        return forwardingRulesByRegion.stream().collect(
            Collectors.toMap(ForwardingRule::getIPAddress, ForwardingRule::getName));
      }
    } catch (Exception e) {
      throw new WingsException(ErrorCode.STACKDRIVER_ERROR).addParam("reason", getMessage(e));
    }
    return Collections.emptyMap();
  }

  @Override
  public String createFilter(StackDriverNameSpace nameSpace, String metric, String dimensionValue) {
    String filter;
    switch (nameSpace) {
      case LOADBALANCER: {
        filter = "metric.type=\"" + metric + "\" AND resource.label.forwarding_rule_name = \"" + dimensionValue + "\"";
        break;
      }
      case POD_NAME: {
        filter = "metric.type=\"" + metric + "\" AND resource.labels.pod_name = \"" + dimensionValue + "\"";
        break;
      }
      default:
        throw new WingsException("Invalid namespace " + nameSpace);
    }
    return filter;
  }

  private ListTimeSeriesResponse getTimeSeriesResponse(Monitoring monitoring, String projectResource, GcpConfig config,
      String filter, long startTime, long endTime, ThirdPartyApiCallLog apiCallLog) {
    String projectId = getProjectId(config);
    apiCallLog.setTitle("Fetching metric data from project " + projectId
        + " for the time range: " + getDateFormatTime(startTime) + " to " + getDateFormatTime(endTime));
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

    apiCallLog.addFieldToRequest(
        ThirdPartyApiCallField.builder().name("body").value(JsonUtils.asJson(filter)).type(FieldType.JSON).build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("Start Time")
                                     .value(getDateFormatTime(startTime))
                                     .type(FieldType.TIMESTAMP)
                                     .build());
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("End Time")
                                     .value(getDateFormatTime(endTime))
                                     .type(FieldType.TIMESTAMP)
                                     .build());

    ListTimeSeriesResponse response;
    try {
      response = monitoring.projects()
                     .timeSeries()
                     .list(projectResource)
                     .setFilter(filter)
                     .setIntervalStartTime(getDateFormatTime(startTime))
                     .setIntervalEndTime(getDateFormatTime(endTime))
                     .execute();
    } catch (IOException e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
      delegateLogService.save(config.getAccountId(), apiCallLog);
      throw new WingsException(
          "Unsuccessful response while fetching data from StackDriver. Error message: " + getMessage(e));
    }
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToResponse(HttpStatus.SC_OK, response, FieldType.JSON);

    delegateLogService.save(config.getAccountId(), apiCallLog);
    return response;
  }

  @Override
  public String getProjectId(GcpConfig gcpConfig) {
    return (String) ((Map) JsonUtils.parseJson(new String(gcpConfig.getServiceAccountKeyFileContent())).json())
        .get("project_id");
  }

  @Override
  public long getTimeStamp(String data) {
    Date date;
    try {
      date = rfc3339.parse(data);
    } catch (ParseException e) {
      throw new WingsException("Unable to convert given timestamp");
    }
    return date.getTime();
  }

  @Override
  public String getDateFormatTime(long time) {
    return rfc3339.format(new Date(time));
  }

  @Override
  public VerificationNodeDataSetupResponse getLogWithDataForNode(GcpConfig gcpConfig,
      List<EncryptedDataDetail> encryptionDetails, String hostName, StackDriverSetupTestNodeData setupTestNodeData,
      ThirdPartyApiCallLog apiCallLog) {
    List<LogEntry> entries;
    List<LogEntry> serviceLevelLoad;
    final long startTime = TimeUnit.SECONDS.toMillis(setupTestNodeData.getFromTime());
    final long endTime = TimeUnit.SECONDS.toMillis(setupTestNodeData.getToTime());
    try {
      // get data without host
      serviceLevelLoad = fetchLogs(setupTestNodeData.getQuery(), startTime, endTime, apiCallLog, Collections.emptySet(),
          setupTestNodeData.getHostnameField(), gcpConfig, encryptionDetails, true, false);

      if (setupTestNodeData.isServiceLevel()) {
        return VerificationNodeDataSetupResponse.builder()
            .providerReachable(true)
            .loadResponse(VerificationLoadResponse.builder()
                              .isLoadPresent(isNotEmpty(serviceLevelLoad))
                              .loadResponse(serviceLevelLoad)
                              .build())
            .build();
      }

      // get data with host
      entries = fetchLogs(setupTestNodeData.getQuery(), startTime, endTime, apiCallLog, Sets.newHashSet(hostName),
          setupTestNodeData.getHostnameField(), gcpConfig, encryptionDetails, false, false);

    } catch (Exception e) {
      logger.error("error fetching logs", e);
      return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
    }
    List<LogElement> logElements = new ArrayList<>();
    int clusterLabel = 0;
    if (isNotEmpty(entries)) {
      logger.info("Total no. of log records found : {}", entries.size());
      for (LogEntry entry : entries) {
        LogElement logElement;
        try {
          String logMessage = JsonPath.read(entry.toString(),
              isNotEmpty(setupTestNodeData.getMessageField()) ? setupTestNodeData.getMessageField()
                                                              : STACKDRIVER_DEFAULT_LOG_MESSAGE_FIELD);
          String host = JsonPath.read(entry.toString(),
              isNotEmpty(setupTestNodeData.getHostnameField()) ? setupTestNodeData.getHostnameField()
                                                               : STACKDRIVER_DEFAULT_HOST_NAME_FIELD);

          if (isEmpty(host) || isEmpty(logMessage)) {
            continue;
          }

          long timeStamp = new DateTime(entry.getTimestamp()).getMillis();
          if (isNotEmpty(logMessage)) {
            logElement = LogElement.builder()
                             .query(setupTestNodeData.getQuery())
                             .logCollectionMinute((int) TimeUnit.MILLISECONDS.toMinutes(timeStamp))
                             .clusterLabel(String.valueOf(clusterLabel++))
                             .count(1)
                             .logMessage(logMessage)
                             .timeStamp(timeStamp)
                             .host(host)
                             .build();
            logElements.add(logElement);
          }
        } catch (Exception e) {
          logger.warn("Unable to parse logEntry due to exception : ", e);
          continue;
        }
      }
    }
    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder()
                          .isLoadPresent(isNotEmpty(serviceLevelLoad))
                          .loadResponse(serviceLevelLoad)
                          .build())
        .dataForNode(logElements)
        .build();
  }

  @Override
  public List<LogEntry> fetchLogs(String query, long startTime, long endTime, ThirdPartyApiCallLog callLog,
      Set<String> hosts, String hostnameField, GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      boolean is24X7Task, boolean fetchNextPage) {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    Logging logging = gcpHelperService.getLoggingResource(gcpConfig, encryptionDetails, projectId);

    String queryField = getQueryField(hostnameField, new ArrayList<>(hosts), query, startTime, endTime, is24X7Task);

    List<LogEntry> logEntries = new ArrayList<>();
    ListLogEntriesResponse response = null;
    String nextPageToken = null;
    boolean hasReachedRateLimit = false;
    do {
      ListLogEntriesRequest request = new ListLogEntriesRequest();
      ThirdPartyApiCallLog apiCallLog = callLog.copy();
      apiCallLog.setTitle("Fetching log data from project " + projectId + " from " + getDateFormatTime(startTime)
          + " to " + getDateFormatTime(endTime));
      apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("query")
                                       .value(queryField)
                                       .type(ThirdPartyApiCallLog.FieldType.JSON)
                                       .build());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("Start Time")
                                       .value(getDateFormatTime(startTime))
                                       .type(ThirdPartyApiCallLog.FieldType.TIMESTAMP)
                                       .build());
      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("End Time")
                                       .value(getDateFormatTime(endTime))
                                       .type(ThirdPartyApiCallLog.FieldType.TIMESTAMP)
                                       .build());
      request.setFilter(queryField);
      request.setProjectIds(Collections.singletonList(projectId));
      request.setPageSize(fetchNextPage ? 1000 : 10);
      if (isNotEmpty(nextPageToken)) {
        request.setPageToken(nextPageToken);
        apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                         .name("Next Page Token")
                                         .value(SecretString.SECRET_MASK)
                                         .type(FieldType.TEXT)
                                         .build());
      }
      try {
        response = logging.entries().list(request).execute();
        hasReachedRateLimit = false;
      } catch (GoogleJsonResponseException ge) {
        if (ge.getStatusCode() == RATE_LIMIT_STATUS) {
          hasReachedRateLimit = true;
          int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
          logger.info("Encountered Rate limiting from stackdriver. Sleeping {} seconds for state {} ", randomNum,
              apiCallLog.getStateExecutionId());
          apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
          apiCallLog.addFieldToResponse(
              HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(ge), ThirdPartyApiCallLog.FieldType.TEXT);
          delegateLogService.save(gcpConfig.getAccountId(), apiCallLog);
          sleep(ofSeconds(randomNum));
          continue;
        } else {
          throw new VerificationOperationException(
              ErrorCode.STACKDRIVER_ERROR, "error fetching logs from stackdriver", ge);
        }
      } catch (Exception e) {
        logger.error("Error fetching logs, request {}", request, e);
        apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        apiCallLog.addFieldToResponse(
            HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), ThirdPartyApiCallLog.FieldType.TEXT);
        delegateLogService.save(gcpConfig.getAccountId(), apiCallLog);
        throw new WingsException("Unsuccessful response while fetching data from StackDriver. Error message: " + e);
      }
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(HttpStatus.SC_OK, response, ThirdPartyApiCallLog.FieldType.JSON);
      delegateLogService.save(gcpConfig.getAccountId(), apiCallLog);
      nextPageToken = response.getNextPageToken();
      if (isNotEmpty(response.getEntries())) {
        logEntries.addAll(response.getEntries());
      }
    } while (hasReachedRateLimit || (fetchNextPage && isNotEmpty(response) && isNotEmpty(response.getNextPageToken())));

    return logEntries;
  }

  private String getQueryField(
      String hostnameField, List<String> hosts, String query, long startTime, long endTime, boolean is24X7Task) {
    String formattedStartTime = getDateFormatTime(startTime);
    String formattedEndTime = getDateFormatTime(endTime);

    StringBuilder queryBuilder = new StringBuilder(80);

    if (!is24X7Task) {
      // for backward compatibility
      if (!hostnameField.contains("resource.labels")) {
        queryBuilder.append("resource.labels.");
      }
      queryBuilder.append(hostnameField).append("=(");
      for (int i = 0; i < hosts.size(); i++) {
        queryBuilder.append(hosts.get(i));
        if (i != hosts.size() - 1) {
          queryBuilder.append(" OR ");
        } else {
          queryBuilder.append(')');
        }
      }
      queryBuilder.append(STACK_DRIVER_QUERY_SEPARATER);
    }
    for (String filter : query.split("\n")) {
      queryBuilder.append(filter).append(STACK_DRIVER_QUERY_SEPARATER);
    }
    queryBuilder.append("timestamp>=\"")
        .append(formattedStartTime)
        .append("\"" + STACK_DRIVER_QUERY_SEPARATER + "timestamp<\"")
        .append(formattedEndTime);

    return queryBuilder.toString() + "\"";
  }

  @Override
  public Object getLogSample(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String query,
      ThirdPartyApiCallLog apiCallLog, long startTime, long endTime) {
    final List<LogEntry> logEntries = fetchLogs(
        query, startTime, endTime, apiCallLog, Collections.emptySet(), null, gcpConfig, encryptionDetails, true, false);
    if (isEmpty(logEntries)) {
      return null;
    }
    return logEntries.get(0);
  }
}
