package io.harness.azure.model;

public interface AzureConstants {
  int DEFAULT_SYNC_AZURE_VMSS_TIMEOUT_MIN = 2;
  String MIN_INSTANCES = "minInstancesExpr";
  String MAX_INSTANCES = "maxInstancesExpr";
  String DESIRED_INSTANCES = "targetInstancesExpr";
  String AUTO_SCALING_VMSS_TIMEOUT = "autoScalingSteadyStateVMSSTimeout";
  String BLUE_GREEN = "blueGreen";
  String AZURE_VMSS_SETUP_COMMAND_NAME = "Azure VMSS Setup";
  String AZURE_VMSS_DEPLOY_COMMAND_NAME = "Resize Azure Virtual Machine Scale Set";
  String ACTIVITY_ID = "activityId";

  int DEFAULT_AZURE_VMSS_MAX_INSTANCES = 10;
  int DEFAULT_AZURE_VMSS_MIN_INSTANCES = 0;
  int DEFAULT_AZURE_VMSS_DESIRED_INSTANCES = 6;
  int DEFAULT_AZURE_VMSS_TIMEOUT_MIN = 10;

  // Command unit Names
  String SETUP_COMMAND_UNIT = "Setup Virtual Machine Scale Set";
  String UP_SCALE_COMMAND_UNIT = "Upscale Virtual Machine Scale Set";
  String UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT = "Upscale wait for steady state";
  String DOWN_SCALE_COMMAND_UNIT = "Downscale Virtual Machine Scale Set";
  String DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT = "Downscale wait for steady state";
  String DEPLOYMENT_STATUS = "Final Deployment status";
  String DELETE_NEW_VMSS = "Delete New Virtual Machine Scale Set";

  // Messaging
  String SKIP_VMSS_DEPLOY = "No Azure VMSS setup context element found. Skipping deploy";
  String SKIP_VMSS_ROLLBACK = "No Azure VMSS setup context element found. Skipping rollback";
}
