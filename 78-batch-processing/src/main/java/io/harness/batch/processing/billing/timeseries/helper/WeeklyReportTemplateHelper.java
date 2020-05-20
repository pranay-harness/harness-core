package io.harness.batch.processing.billing.timeseries.helper;

import java.util.Map;

public class WeeklyReportTemplateHelper {
  private static final String CLUSTER_COST_NOT_AVAILABLE = "<span style='color: #DBDCDD;font-size: 40px;'>-</span>";
  private static final String TOTAL_CLUSTER_COST_AVAILABLE =
      "<span style=\"color: %s; font-size: 14px;text-align: center\">%s | %s</span><br><span style=\"color: #00ade4;font-size: 40px;\">%s</span><br><span style=\"font-size: 10px;\">Cluster cost</span>";
  private static final String COST_NOT_AVAILABLE = "";
  private static final String APPLICATION_RELATED_COSTS_AVAILABLE =
      "<tr><td align=\"left\" valign=\"top\" style=\"-webkit-font-smoothing: antialiased; text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; -webkit-text-size-adjust: 100%%; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0; margin: 0; padding: 0; padding-left: 7%%; padding-right: 6.25%%; width: 87.5%%; font-size: 17px; font-weight: 400; line-height: 160%%; padding-bottom: 25px; color: #000000; font-family: 'Source Sans Pro', Tahoma, Verdana, Segoe, sans-serif; border-collapse: collapse;\" class=\"paragraph\" width=\"87.5%%\"><h1 style=\"font-size: 18px; font-weight: normal;\">Applications</h1><h1 style=\"font-size: 14px; font-weight: normal;\">%s</h1></td></tr>";
  private static final String CLUSTER_RELATED_COSTS_AVAILABLE =
      "<tr><td align=\"left\" valign=\"top\" style=\"-webkit-font-smoothing: antialiased; text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; -webkit-text-size-adjust: 100%%; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border-spacing: 0; margin: 0; padding: 0; padding-left: 7%%; padding-right: 6.25%%; width: 87.5%%; font-size: 17px; font-weight: 400; line-height: 160%%; padding-bottom: 25px; color: #000000; font-family: 'Source Sans Pro', Tahoma, Verdana, Segoe, sans-serif; border-collapse: collapse;\" class=\"paragraph\" width=\"87.5%%\"><h1 style=\"font-size: 18px; font-weight: normal;\">Clusters</h1><h1 style=\"font-size: 14px; font-weight: normal;\">%s</h1></td></tr>";
  private static final String ENTITY_COST_AVAILABLE =
      "<span>%s: <span style=\"color: #00ade4\">%s </span>%s <span style=\"color: %s; font-size: 13px;\">(%s | %s)</span><br>";

  public static final String TOTAL_CLUSTER_COST = "TOTAL_CLUSTER";
  public static final String TOTAL_CLUSTER_IDLE_COST = "TOTAL_CLUSTER_IDLE";
  public static final String TOTAL_CLUSTER_UNALLOCATED_COST = "TOTAL_CLUSTER_UNALLOCATED";
  public static final String CLUSTER_RELATED_COSTS = "CLUSTER_RELATED_COSTS";
  public static final String CLUSTER = "Cluster";
  public static final String NAMESPACE = "Namespace";
  public static final String WORKLOAD = "Workload";
  public static final String APPLICATION_RELATED_COSTS = "APPLICATION_RELATED_COSTS";
  public static final String APPLICATION = "Application";
  public static final String SERVICE = "Service";
  public static final String ENVIRONMENT = "Environment";

  public static final String NAME = "_NAME";
  public static final String COST = "_COST";
  public static final String COST_CHANGE_PERCENT = "_COST_CHANGE_PERCENT";
  public static final String COST_DIFF_AMOUNT = "_COST_DIFF";
  public static final String COST_TREND = "_COST_TREND";
  public static final String COST_AVAILABLE = "_COST_AVAILABLE";

  public static final String DECREASE = "DECREASE";
  public static final String INCREASE = "INCREASE";
  public static final String AVAILABLE = "AVAILABLE";
  public static final String NOT_AVAILABLE = "NOT_AVAILABLE";

  public void populateCostDataForTemplate(Map<String, String> templateModel, Map<String, String> values) {
    templateModel.put("TOTAL_CLUSTER_COST", getTotalCostPopulatedValue(TOTAL_CLUSTER_COST, values));
    templateModel.put("TOTAL_CLUSTER_IDLE_COST", getTotalCostPopulatedValue(TOTAL_CLUSTER_IDLE_COST, values));
    templateModel.put(
        "TOTAL_CLUSTER_UNALLOCATED_COST", getTotalCostPopulatedValue(TOTAL_CLUSTER_UNALLOCATED_COST, values));
    templateModel.put(APPLICATION_RELATED_COSTS, COST_NOT_AVAILABLE);
    templateModel.put(CLUSTER_RELATED_COSTS, COST_NOT_AVAILABLE);
    if (values.get(APPLICATION_RELATED_COSTS).equals(AVAILABLE)) {
      templateModel.put(APPLICATION_RELATED_COSTS,
          String.format(APPLICATION_RELATED_COSTS_AVAILABLE,
              getEntityCostPopulatedValue(APPLICATION, values) + getEntityCostPopulatedValue(SERVICE, values)
                  + getEntityCostPopulatedValue(ENVIRONMENT, values)));
    }
    if (values.get(CLUSTER_RELATED_COSTS).equals(AVAILABLE)) {
      templateModel.put(CLUSTER_RELATED_COSTS,
          String.format(CLUSTER_RELATED_COSTS_AVAILABLE,
              getEntityCostPopulatedValue(CLUSTER, values) + getEntityCostPopulatedValue(NAMESPACE, values)
                  + getEntityCostPopulatedValue(WORKLOAD, values)));
    }
  }

  private String getTotalCostPopulatedValue(String entity, Map<String, String> values) {
    if (values.get(entity + COST_AVAILABLE).equals(AVAILABLE)) {
      String color = "green";
      if (values.get(entity + COST_TREND).equals(INCREASE)) {
        color = "red";
      }
      return String.format(TOTAL_CLUSTER_COST_AVAILABLE, color, values.get(entity + COST_CHANGE_PERCENT),
          values.get(entity + COST_DIFF_AMOUNT), values.get(entity + COST));
    } else {
      return CLUSTER_COST_NOT_AVAILABLE;
    }
  }

  private String getEntityCostPopulatedValue(String entity, Map<String, String> values) {
    if (values.get(entity + COST_AVAILABLE).equals(AVAILABLE)) {
      String color = "green";
      if (values.get(entity + COST_TREND).equals(INCREASE)) {
        color = "red";
      }
      return String.format(ENTITY_COST_AVAILABLE, entity, values.get(entity + NAME), values.get(entity + COST), color,
          values.get(entity + COST_CHANGE_PERCENT), values.get(entity + COST_DIFF_AMOUNT));
    } else {
      return COST_NOT_AVAILABLE;
    }
  }
}
