package software.wings.service.impl.stackdriver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.LOADBALANCER;
import static software.wings.service.impl.stackdriver.StackDriverNameSpace.VMINSTANCE;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.MonitoringScopes;
import com.google.api.services.monitoring.v3.model.ListTimeSeriesResponse;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by Pranjal on 11/27/2018
 */
@Singleton
public class StackDriverDelegateServiceImpl implements StackDriverDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(StackDriverDelegateServiceImpl.class);

  private static final FastDateFormat rfc3339 =
      FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"));

  @Inject private EncryptionService encryptionService;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(GcpConfig gcpConfig,
      List<EncryptedDataDetail> encryptionDetails, StackDriverSetupTestNodeData setupTestNodeData, String hostName,
      ThirdPartyApiCallLog apiCallLog) throws IOException {
    encryptionService.decrypt(gcpConfig, encryptionDetails);
    String projectId = getProjectId(gcpConfig);
    Monitoring monitoring = getMonitoringClient(gcpConfig, projectId);
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

  public Monitoring getMonitoringClient(GcpConfig gcpConfig, String projectId) throws IOException {
    HttpTransport httpTransport = new NetHttpTransport();
    JsonFactory jsonFactory = new JacksonFactory();
    GoogleCredential credentials =
        GoogleCredential
            .fromStream(new ByteArrayInputStream(
                Charset.forName("UTF-8").encode(CharBuffer.wrap(gcpConfig.getServiceAccountKeyFileContent())).array()))
            .createScoped(MonitoringScopes.all());

    return new Monitoring.Builder(httpTransport, jsonFactory, credentials).setApplicationName(projectId).build();
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
