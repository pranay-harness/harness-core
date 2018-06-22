package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.sm.states.AbstractAnalysisState.END_TIME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.HOST_NAME_PLACE_HOLDER;
import static software.wings.sm.states.AbstractAnalysisState.START_TIME_PLACE_HOLDER;

import io.harness.data.structure.EmptyPredicate;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.metrics.MetricType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.analysis.TimeSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class PrometheusResource {
  @POST
  @Path("validate-metrics")
  public RestResponse<Map<String, String>> validateMetrics(
      @QueryParam("accountId") final String accountId, @NotEmpty List<TimeSeries> timeSeriesToAnalyze) {
    return new RestResponse<>(validateTransactions(accountId, timeSeriesToAnalyze));
  }

  public static Map<String, String> validateTransactions(final String accountId, List<TimeSeries> timeSeriesToAnalyze) {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(timeSeriesToAnalyze)) {
      invalidFields.put("timeSeriesToAnalyze", "No metrics given to analyze.");
      return invalidFields;
    }
    Map<String, MetricType> metricNameToType = new HashMap<>();
    timeSeriesToAnalyze.forEach(timeSeries -> {
      List<String> missingPlaceHolders = new ArrayList<>();
      if (isEmpty(timeSeries.getUrl()) || !timeSeries.getUrl().contains(HOST_NAME_PLACE_HOLDER)) {
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
}
