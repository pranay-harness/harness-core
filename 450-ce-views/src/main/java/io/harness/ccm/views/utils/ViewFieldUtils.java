package io.harness.ccm.views.utils;

import io.harness.ccm.views.graphql.QLCEViewField;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class ViewFieldUtils {
  public static List<QLCEViewField> getAwsFields() {
    return ImmutableList.of(QLCEViewField.builder().fieldId("awsServicecode").fieldName("Service").build(),
        QLCEViewField.builder().fieldId("awsUsageAccountId").fieldName("Account").build(),
        QLCEViewField.builder().fieldId("awsInstancetype").fieldName("Instance Type").build(),
        QLCEViewField.builder().fieldId("awsUsagetype").fieldName("Usage Type").build());
  }
  public static List<QLCEViewField> getGcpFields() {
    return ImmutableList.of(QLCEViewField.builder().fieldId("gcpProduct").fieldName("Product").build(),
        QLCEViewField.builder().fieldId("gcpProjectId").fieldName("Project").build(),
        QLCEViewField.builder().fieldId("gcpSkuDescription").fieldName("SKUs").build());
  }
  public static List<QLCEViewField> getClusterFields() {
    return ImmutableList.of(QLCEViewField.builder().fieldId("clusterName").fieldName("Cluster Name").build(),
        QLCEViewField.builder().fieldId("namespace").fieldName("Namespace").build(),
        QLCEViewField.builder().fieldId("workloadName").fieldName("Workload").build(),
        QLCEViewField.builder().fieldId("appId").fieldName("Application").build(),
        QLCEViewField.builder().fieldId("envId").fieldName("Environment").build(),
        QLCEViewField.builder().fieldId("serviceId").fieldName("Service").build());
  }
  public static List<QLCEViewField> getCommonFields() {
    return ImmutableList.of(QLCEViewField.builder().fieldId("region").fieldName("Region").build(),
        QLCEViewField.builder().fieldId("product").fieldName("Product").build(),
        QLCEViewField.builder().fieldId("label").fieldName("Label").build());
  }
}
