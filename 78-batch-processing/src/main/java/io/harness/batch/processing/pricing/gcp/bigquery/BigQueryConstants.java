package io.harness.batch.processing.pricing.gcp.bigquery;

public class BigQueryConstants {
  private BigQueryConstants() {}

  public static final String AWS_EC2_BILLING_QUERY =
      "SELECT SUM(unblendedcost) as cost, resourceid, servicecode, productfamily  "
      + "FROM `%s` "
      + "WHERE resourceid IN "
      + "( '%s' )  AND "
      + "usagestartdate  >= '%s' AND usagestartdate < '%s' "
      + "GROUP BY  resourceid, servicecode, productfamily; ";

  public static final String cost = "cost";
  public static final String resourceId = "resourceid";
  public static final String serviceCode = "servicecode";
  public static final String productFamily = "productfamily";

  public static final String networkProductFamily = "Data Transfer";
  public static final String computeProductFamily = "Compute Instance";
}
