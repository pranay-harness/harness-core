package software.wings.service.impl.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.ExceptionUtils.getMessage;
import static software.wings.common.VerificationConstants.STACK_DRIVER_QUERY_SEPARATER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.LOADBALANCER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.POD_NAME;

import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.Region;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.model.ListLogEntriesRequest;
import com.google.api.services.logging.v2.model.ListLogEntriesResponse;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import software.wings.beans.GcpConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.security.encryption.EncryptedDataDetail;
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

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(
            VerificationLoadResponse.builder().isLoadPresent(isNotEmpty(responses)).loadResponse(responses).build())
        .dataForNode(responses)
        .build();
  }

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
    apiCallLog.setTitle("Fetching metric data from project");
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

  public String getProjectId(GcpConfig gcpConfig) {
    return (String) ((Map) JsonUtils.parseJson(new String(gcpConfig.getServiceAccountKeyFileContent())).json())
        .get("project_id");
  }

  public long getTimeStamp(String data) {
    Date date;
    try {
      date = rfc3339.parse(data);
    } catch (ParseException e) {
      throw new WingsException("Unable to convert given timestamp");
    }
    return date.getTime();
  }

  public String getDateFormatTime(long time) {
    return rfc3339.format(new Date(time));
  }

  public List<LogElement> getLogWithDataForNode(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String query, long startTime, long endTime, ThirdPartyApiCallLog apiCallLog, Set<String> hosts,
      String hostnameField, long logCollectionMinute, boolean is24X7Task) {
    List<LogEntry> entries = fetchLogs(
        query, startTime, endTime, apiCallLog, hosts, hostnameField, gcpConfig, encryptionDetails, is24X7Task);
    List<LogElement> logElements = new ArrayList<>();
    int clusterLabel = 0;
    if (isNotEmpty(entries)) {
      logger.info("Total no. of log records found : {}", entries.size());
      for (LogEntry entry : entries) {
        long timeStamp = new DateTime(entry.getTimestamp()).getMillis();
        LogElement logElement = LogElement.builder()
                                    .query(query)
                                    .logCollectionMinute(is24X7Task ? (int) TimeUnit.MILLISECONDS.toMinutes(timeStamp)
                                                                    : logCollectionMinute)
                                    .clusterLabel(String.valueOf(clusterLabel++))
                                    .count(1)
                                    .logMessage(entry.getTextPayload())
                                    .timeStamp(timeStamp)
                                    .host(entry.getResource().getLabels().get(hostnameField))
                                    .build();
        logElements.add(logElement);
      }
    }
    return logElements;
  }

  public List<LogEntry> fetchLogs(String query, long startTime, long endTime, ThirdPartyApiCallLog apiCallLog,
      Set<String> hosts, String hostnameField, GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      boolean is24X7Task) {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    Logging logging = gcpHelperService.getLoggingResource(gcpConfig, encryptionDetails, projectId);
    apiCallLog.setTitle("Fetching log data from project");
    apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());

    String queryField = getQueryField(hostnameField, new ArrayList<>(hosts), query, startTime, endTime, is24X7Task);

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

    ListLogEntriesResponse response;
    try {
      ListLogEntriesRequest request = new ListLogEntriesRequest();
      request.setFilter(queryField);
      request.setProjectIds(Collections.singletonList(projectId));
      response = logging.entries().list(request).execute();
    } catch (Exception e) {
      apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
      apiCallLog.addFieldToResponse(
          HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), ThirdPartyApiCallLog.FieldType.TEXT);
      delegateLogService.save(gcpConfig.getAccountId(), apiCallLog);
      throw new WingsException("Unsuccessful response while fetching data from StackDriver. Error message: " + e);
    }
    apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
    apiCallLog.addFieldToResponse(HttpStatus.SC_OK, response, ThirdPartyApiCallLog.FieldType.JSON);
    delegateLogService.save(gcpConfig.getAccountId(), apiCallLog);

    return response.getEntries();
  }
  private String getQueryField(
      String hostnameField, List<String> hosts, String query, long startTime, long endTime, boolean is24X7Task) {
    String formattedStartTime = getDateFormatTime(startTime);
    String formattedEndTime = getDateFormatTime(endTime);

    StringBuilder queryBuilder = new StringBuilder(80);

    if (!is24X7Task) {
      queryBuilder.append("resource.labels." + hostnameField + "=(");
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
}
