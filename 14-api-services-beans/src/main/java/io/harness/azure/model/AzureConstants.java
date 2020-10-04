package io.harness.azure.model;

import java.util.concurrent.TimeUnit;

public interface AzureConstants {
  int DEFAULT_SYNC_AZURE_VMSS_TIMEOUT_MIN = 2;
  String NEW_VIRTUAL_MACHINE_SCALE_SET = "New Virtual Machine Scale Set";
  String OLD_VIRTUAL_MACHINE_SCALE_SET = "Old Virtual Machine Scale Set";
  String STAGE_BACKEND_POOL = "Stage Backend Pool";
  String PROD_BACKEND_POOL = "Production Backend Pool";
  String MIN_INSTANCES = "minInstancesExpr";
  String MAX_INSTANCES = "maxInstancesExpr";
  String DESIRED_INSTANCES = "targetInstancesExpr";
  String AUTO_SCALING_VMSS_TIMEOUT = "autoScalingSteadyStateVMSSTimeout";
  String BLUE_GREEN = "blueGreen";
  String AZURE_VMSS_SETUP_COMMAND_NAME = "Azure VMSS Setup";
  String AZURE_VMSS_DEPLOY_COMMAND_NAME = "Resize Azure Virtual Machine Scale Set";
  String AZURE_VMSS_SWAP_BACKEND_POOL = "Swap VMSS Backend Pool";
  String ACTIVITY_ID = "activityId";
  int NUMBER_OF_LATEST_VERSIONS_TO_KEEP = 3;
  String STEADY_STATE_TIMEOUT_REGEX = "w|d|h|m|s|ms";

  // VMSS Tags names and values
  String HARNESS_AUTOSCALING_GROUP_TAG_NAME = "HARNESS_REVISION";
  String DYNAMIC_BASE_VMSS_PROVISIONING_PREFIX = "azure:";
  String NAME_TAG = "Name";
  String BG_VERSION_TAG_NAME = "BG_VERSION";
  String BG_GREEN_TAG_VALUE = "GREEN";
  String BG_BLUE_TAG_VALUE = "BLUE";
  String VMSS_CREATED_TIME_STAMP_TAG_NAME = "Created";

  // User VM Auth types
  String VMSS_AUTH_TYPE_DEFAULT = "PASSWORD";
  String VMSS_AUTH_TYPE_SSH_PUBLIC_KEY = "SSH_PUBLIC_KEY";

  // Default Azure VMSS values
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
  String DEPLOYMENT_ERROR = "Failed Deployment status";
  String DELETE_NEW_VMSS = "Delete New Virtual Machine Scale Set";
  String BG_SWAP_ROUTES_COMMAND_UNIT = "Swap Routes";
  String BG_ROLLBACK_COMMAND_UNIT = "Rollback Swap Routes";

  // Messaging
  String SKIP_VMSS_DEPLOY = "No Azure VMSS setup context element found. Skipping deploy";
  String SKIP_VMSS_ROLLBACK = "No Azure VMSS setup context element found. Skipping rollback";
  String SKIP_RESIZE_SCALE_SET = "No scale set found with the name = [%s], hence skipping";
  String REQUEST_DELETE_SCALE_SET = "Sending request to delete newly created Virtual Machine Scale Set: [%s]";
  String SUCCESS_DELETE_SCALE_SET = "Virtual Machine Scale Set: [%s] deleted successfully";
  String SWAP_ROUTE_FAILURE = "Azure Virtual Machine Scale Set swap route failed with error ";
  String SETUP_ELEMENT_NOT_FOUND = "Did not find Setup element of class AzureVMSSSetupContextElement";

  // Validation messages
  String RESOURCE_GROUP_NAME_NULL_VALIDATION_MSG = "Parameter resourceGroupName is required and cannot be null";
  String RESOURCE_ID_NAME_NULL_VALIDATION_MSG = "Parameter resourceId is required and cannot be null";
  String LOAD_BALANCER_NAME_NULL_VALIDATION_MSG = "Parameter loadBalancerName is required and cannot be null";
  String BACKEND_POOL_NAME_NULL_VALIDATION_MSG = "Parameter backendPoolName is required and cannot be null";
  String TARGET_RESOURCE_ID_NULL_VALIDATION_MSG = "Parameter targetResourceId is required and cannot be null";
  String AUTOSCALE_SETTINGS_RESOURCE_JSON_NULL_VALIDATION_MSG =
      "Parameter autoScaleSettingResourceInnerJson is required and cannot be null";
  String AZURE_MANAGEMENT_CLIENT_NULL_VALIDATION_MSG = "Azure management client can't be null";
  String VIRTUAL_MACHINE_SCALE_SET_NULL_VALIDATION_MSG =
      "Parameter virtualMachineScaleSet is required and cannot be null";
  String PRIMARY_INTERNET_FACING_LOAD_BALANCER_NULL_VALIDATION_MSG =
      "Parameter primaryInternetFacingLoadBalancer is required and cannot be null";
  String SUBSCRIPTION_ID_NULL_VALIDATION_MSG = "Parameter subscriptionId is required and cannot be null";
  String VIRTUAL_MACHINE_SCALE_SET_ID_NULL_VALIDATION_MSG =
      "Parameter virtualMachineScaleSetId is required and cannot be null";
  String VIRTUAL_SCALE_SET_NAME_NULL_VALIDATION_MSG = "Parameter virtualScaleSetName is required and cannot be null";
  String BASE_VIRTUAL_MACHINE_SCALE_SET_IS_NULL_VALIDATION_MSG =
      "Parameter baseVirtualMachineScaleSet is required and cannot be null";
  long AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);
  String NEW_VIRTUAL_MACHINE_SCALE_SET_NAME_IS_NULL_VALIDATION_MSG =
      "Parameter newVirtualMachineScaleSetName is required and cannot be null";
  String HARNESS_REVISION_IS_NULL_VALIDATION_MSG = "Parameter harnessRevision is required and cannot be null";
  String VMSS_IDS_IS_NULL_VALIDATION_MSG = "Parameter vmssIds is required and cannot be null";
  String NUMBER_OF_VM_INSTANCES_VALIDATION_MSG = "Required number of VM instances can't have negative value";
  String BACKEND_POOLS_LIST_EMPTY_VALIDATION_MSG = "Backend pools list cannot be empty";
  String VM_INSTANCE_IDS_LIST_EMPTY_VALIDATION_MSG = "Virtual Machine instances ids list cannot be empty";
  String VM_INSTANCE_IDS_NOT_NUMBERS_VALIDATION_MSG = "Virtual Machine instances ids must be '*' or numbers";
  String NEW_VMSS_NAME_NULL_VALIDATION_MSG = "Parameter newVMSSName is required and cannot be null";
  String OLD_VMSS_NAME_NULL_VALIDATION_MSG = "Parameter oldVMSSName is required and cannot be null";
  String AZURE_LOAD_BALANCER_DETAIL_NULL_VALIDATION_MSG =
      "Parameter azureLoadBalancerDetail is required and cannot be null";
  String UNRECOGNIZED_PARAMETERS = "Parameters of unrecognized class: [%s] found while executing deploy step.";
  String UNRECOGNIZED_TASK = "Unrecognized task params while running azure vmss task: [%s]";
  String GALLERY_NAME_NULL_VALIDATION_MSG = "Parameter galleryName is required and cannot be null";
  String GALLERY_IMAGE_NAME_NULL_VALIDATION_MSG = "Parameter imageName is required and cannot be null";

  String GALLERY_IMAGE_ID_PATTERN =
      "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/galleries/%s/images/%s/versions/%s";
}
