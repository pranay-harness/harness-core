package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.data.structure.EmptyPredicate;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.prometheus.PrometheusSetupTestNodeData;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.service.intfc.prometheus.PrometheusAnalysisService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 4/24/18.
 */
@Api("prometheus")
@Path("/prometheus")
@Produces("application/json")
@Scope(ResourceType.SETTING)
public class PrometheusResource implements LogAnalysisResource {
  @Inject private PrometheusAnalysisService analysisService;

  @POST
  @Path("validate-metrics")
  public RestResponse<Map<String, String>> validateMetrics(
      @QueryParam("accountId") final String accountId, @NotEmpty List<TimeSeries> timeSeriesToAnalyze) {
    return new RestResponse<>(validateTransactions(timeSeriesToAnalyze, false));
  }

  public static Map<String, String> validateTransactions(List<TimeSeries> timeSeriesToAnalyze, boolean serviceLevel) {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(timeSeriesToAnalyze)) {
      invalidFields.put("timeSeriesToAnalyze", "No metrics given to analyze.");
      return invalidFields;
    }
    Map<String, String> metricNameToType = new HashMap<>();
    timeSeriesToAnalyze.forEach(timeSeries -> {
      List<String> missingPlaceHolders = new ArrayList<>();

      if (!serviceLevel && (isEmpty(timeSeries.getUrl()) || !timeSeries.getUrl().contains(HOST_NAME_PLACE_HOLDER))) {
        missingPlaceHolders.add(HOST_NAME_PLACE_HOLDER);
      }

      if (isEmpty(timeSeries.getUrl()) || !timeSeries.getUrl().contains(START_TIME_PLACE_HOLDER)) {
        missingPlaceHolders.add(START_TIME_PLACE_HOLDER);
      }

      if (isEmpty(timeSeries.getUrl()) || !timeSeries.getUrl().contains(END_TIME_PLACE_HOLDER)) {
        missingPlaceHolders.add(END_TIME_PLACE_HOLDER);
      }

      if (EmptyPredicate.isNotEmpty(missingPlaceHolders)) {
        invalidFields.put(
            "Invalid url for txn: " + timeSeries.getTxnName() + ", metric : " + timeSeries.getMetricName(),
            missingPlaceHolders + " are not present in the url.");
      }

      if (metricNameToType.get(timeSeries.getMetricName()) == null) {
        metricNameToType.put(timeSeries.getMetricName(), timeSeries.getMetricType());
      } else if (!metricNameToType.get(timeSeries.getMetricName()).equals(timeSeries.getMetricType())) {
        invalidFields.put(
            "Invalid metric type for txn: " + timeSeries.getTxnName() + ", metric : " + timeSeries.getMetricName(),
            timeSeries.getMetricName() + " has been configured as " + metricNameToType.get(timeSeries.getMetricName())
                + " in previous transactions. Same metric name can not have different metric types.");
      }
    });
    return invalidFields;
  }

  /**
   * Api to fetch Metric data for given node.
   * @param accountId
   * @param setupTestNodeData
   * @return
   */
  @POST
  @Path("/node-data")
  @Timed
  @DelegateAuth
  @ExceptionMetered
  public RestResponse<VerificationNodeDataSetupResponse> getMetricsWithDataForNode(
      @QueryParam("accountId") final String accountId, @Valid PrometheusSetupTestNodeData setupTestNodeData) {
    validateTransactions(setupTestNodeData.getTimeSeriesToAnalyze(), setupTestNodeData.isServiceLevel());
    return new RestResponse<>(analysisService.getMetricsWithDataForNode(setupTestNodeData));
  }
}
