package software.wings.service.impl.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.LOADBALANCER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.VMINSTANCE;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.Region;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import software.wings.beans.GcpConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(GcpConfig gcpConfig,
      List<EncryptedDataDetail> encryptionDetails, StackDriverSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    Monitoring monitoring = gcpHelperService.getMonitoringService(gcpConfig, encryptionDetails, projectId);
    String projectResource = "projects/" + projectId;
    List<ListTimeSeriesResponse> responses = new ArrayList<>();

    if (!isEmpty(setupTestNodeData.getVmInstanceMetrics())) {
      setupTestNodeData.getVmInstanceMetrics().forEach(metric -> {
        String vmFilter = createFilter(VMINSTANCE, metric.getMetricName(), hostName, null);
        ListTimeSeriesResponse response = getTimeSeriesResponse(
            monitoring, projectResource, vmFilter, setupTestNodeData.getFromTime(), setupTestNodeData.getToTime());
        responses.add(response);
      });
    }

    if (!isEmpty(setupTestNodeData.getLoadBalancerMetrics())) {
      setupTestNodeData.getLoadBalancerMetrics().forEach((ruleName, lbMetrics) -> lbMetrics.forEach(lbMetric -> {
        String lbFilter = createFilter(LOADBALANCER, lbMetric.getMetricName(), hostName, ruleName);
        ListTimeSeriesResponse response = getTimeSeriesResponse(
            monitoring, projectResource, lbFilter, setupTestNodeData.getFromTime(), setupTestNodeData.getToTime());
        responses.add(response);
      }));
    }

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder().isLoadPresent(true).loadResponse(responses).build())
        .dataForNode(responses)
        .build();
  }

  public List<String> listRegions(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    List<Region> regions = gcpHelperService.getGCEService(gcpConfig, encryptionDetails, projectId)
                               .regions()
                               .list(projectId)
                               .execute()
                               .getItems();
    return regions.stream().map(Region::getName).collect(Collectors.toList());
  }

  public Map<String, String> listForwardingRules(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String region) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    Compute gceService = gcpHelperService.getGCEService(gcpConfig, encryptionDetails, projectId);
    gceService.forwardingRules().list(getProjectId(gcpConfig), region).execute();
    List<ForwardingRule> forwardingRules = gcpHelperService.getGCEService(gcpConfig, encryptionDetails, projectId)
                                               .forwardingRules()
                                               .list(projectId, "us-central1")
                                               .execute()
                                               .getItems();
    return forwardingRules.stream().collect(Collectors.toMap(ForwardingRule::getIPAddress, ForwardingRule::getName));
  }

  public String createFilter(StackDriverNameSpace nameSpace, String metric, String hostName, String ruleName) {
    String filter;
    switch (nameSpace) {
      case LOADBALANCER: {
        filter = "metric.type=\"" + metric + "\" AND resource.label.forwarding_rule_name = \"" + ruleName + "\"";
        break;
      }
      case VMINSTANCE: {
        filter = "metric.type=\"" + metric + "\" AND metric.labels.instance_name = \"" + hostName + "\"";
        break;
      }
      default:
        throw new WingsException("Invalid namespace " + nameSpace);
    }
    return filter;
  }

  private ListTimeSeriesResponse getTimeSeriesResponse(
      Monitoring monitoring, String projectResource, String filter, long startTime, long endTime) {
    try {
      return monitoring.projects()
          .timeSeries()
          .list(projectResource)
          .setFilter(filter)
          .setIntervalStartTime(getDateFormatTime(startTime))
          .setIntervalEndTime(getDateFormatTime(endTime))
          .execute();
    } catch (IOException e) {
      logger.warn("Metric not found " + filter + e.getMessage());
    }
    return null;
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
}
