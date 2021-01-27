package io.harness.batch.processing.pricing.gcp.bigquery;

public class BigQueryConstants {
  private BigQueryConstants() {}

  public static final String AWS_EC2_BILLING_QUERY =
      "SELECT SUM(blendedcost) as cost, sum(effectivecost) as effectivecost, resourceid, servicecode, productfamily  "
      + "FROM `%s` "
      + "WHERE resourceid IN "
      + "( '%s' )  AND "
      + "usagestartdate  >= '%s' AND usagestartdate < '%s' "
      + "GROUP BY  resourceid, servicecode, productfamily; ";

  public static final String AWS_BILLING_DATA = "SELECT resourceid, productfamily  "
      + "FROM `%s` "
      + "WHERE productfamily = 'Compute Instance' AND "
      + "usagestartdate  >= '%s' AND usagestartdate < '%s' LIMIT 1";

  public static final String CLOUD_PROVIDER_AGG_DATA =
      "SELECT count(*) AS count, cloudProvider FROM `%s` GROUP BY cloudProvider";

  public static final String cost = "cost";
  public static final String effectiveCost = "effectivecost";
  public static final String resourceId = "resourceid";
  public static final String serviceCode = "servicecode";
  public static final String productFamily = "productfamily";

  public static final String networkProductFamily = "Data Transfer";
  public static final String computeProductFamily = "Compute Instance";
}
