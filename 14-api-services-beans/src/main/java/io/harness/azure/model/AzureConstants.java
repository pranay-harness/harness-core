package io.harness.azure.model;

public interface AzureConstants {
  int defaultSyncAzureVMSSTimeoutMin = 2;
  String MIN_INSTANCES = "minInstancesExpr";
  String MAX_INSTANCES = "maxInstancesExpr";
  String DESIRED_INSTANCES = "targetInstancesExpr";
  String AUTO_SCALING_VMSS_TIMEOUT = "autoScalingSteadyStateVMSSTimeout";
  String BLUE_GREEN = "blueGreen";

  int DEFAULT_AZURE_VMSS_MAX_INSTANCES = 10;
  int DEFAULT_AZURE_VMSS_MIN_INSTANCES = 0;
  int DEFAULT_AZURE_VMSS_DESIRED_INSTANCES = 6;
  int DEFAULT_AZURE_VMSS_TIMEOUT_MIN = 10;
}
