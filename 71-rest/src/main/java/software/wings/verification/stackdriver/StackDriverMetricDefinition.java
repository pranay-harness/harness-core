package software.wings.verification.stackdriver;

import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class StackDriverMetricDefinition {
  String metricType;
  String metricName;
  String txnName;
  String filterJson;
  String filter;
  List<String> groupByFields;
  String perSeriesAligner;

  public void extractJson() {
    try {
      setFilter();
      setGrouping();
      setPerSeriesAligner();
    } catch (Exception ex) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Exception while parsing JSON Query", ex);
    }
  }

  public void setGrouping() {
    JSONObject metric = new JSONObject(filterJson);
    if (metric.get("dataSets") == null) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Stackdriver JSON does not contain dataSets field");
    }
    JSONArray dataSets = (JSONArray) metric.get("dataSets");
    if (dataSets.length() > 1) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Stackdriver JSON contains more than one dataset");
    }
    JSONObject timeSeriesFilterObj = (JSONObject) dataSets.get(0);
    JSONArray groupArray = ((JSONObject) timeSeriesFilterObj.get("timeSeriesFilter")).getJSONArray("groupByFields");
    this.groupByFields = new ArrayList<>();
    int counter = 0;
    while (counter < groupArray.length()) {
      this.groupByFields.add(groupArray.getString(counter++));
    }
  }

  public void setPerSeriesAligner() {
    JSONObject metric = new JSONObject(filterJson);
    if (metric.get("dataSets") == null) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Stackdriver JSON does not contain dataSets field");
    }
    JSONArray dataSets = (JSONArray) metric.get("dataSets");
    if (dataSets.length() > 1) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Stackdriver JSON contains more than one dataset");
    }
    JSONObject timeSeriesFilterObj = (JSONObject) dataSets.get(0);
    this.perSeriesAligner = ((JSONObject) timeSeriesFilterObj.get("timeSeriesFilter")).getString("perSeriesAligner");
  }

  public void setFilter() {
    JSONObject metric = new JSONObject(filterJson);
    if (metric.get("dataSets") == null) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Stackdriver JSON does not contain dataSets field");
    }
    JSONArray dataSets = (JSONArray) metric.get("dataSets");
    if (dataSets.length() > 1) {
      throw new VerificationOperationException(
          ErrorCode.APM_CONFIGURATION_ERROR, "Stackdriver JSON contains more than one dataset");
    }
    JSONObject timeSeriesFilterObj = (JSONObject) dataSets.get(0);
    this.filter = ((JSONObject) timeSeriesFilterObj.get("timeSeriesFilter")).getString("filter");
  }
}
