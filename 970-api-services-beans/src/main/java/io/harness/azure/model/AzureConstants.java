package io.harness.azure.model;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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
  String TRAFFIC_WEIGHT_EXPR = "trafficWeightExpr";
  String BLUE_GREEN = "blueGreen";
  String AZURE_VMSS_SETUP_COMMAND_NAME = "Azure VMSS Setup";
  String AZURE_VMSS_DEPLOY_COMMAND_NAME = "Resize Azure Virtual Machine Scale Set";
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
  String DELETE_OLD_VIRTUAL_MACHINE_SCALE_SETS_COMMAND_UNIT = "Delete Old Virtual Machine Scale Sets";
  String DOWN_SCALE_COMMAND_UNIT = "Downscale Virtual Machine Scale Set";
  String DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT = "Downscale wait for steady state";
  String CREATE_NEW_VMSS_COMMAND_UNIT = "Create New Virtual Machine Scale Set";
  String DEPLOYMENT_STATUS = "Final Deployment status";
  String DEPLOYMENT_ERROR = "Failed Deployment status";
  String DELETE_NEW_VMSS = "Delete New Virtual Machine Scale Set";
  String AZURE_VMSS_SWAP_BACKEND_POOL = "Swap VMSS Backend Pool";
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
  String NO_VMSS_FOR_DOWN_SIZING = "No old Virtual Machine Scale Set was found with non-zero capacity for down scale";
  String NO_VMSS_FOR_DELETION = "No old Virtual Machine Scale Set was found for deletion";
  String NO_SCALING_POLICY_DURING_DOWN_SIZING = "Not attaching scaling policy to VMSS: [%s] while down sizing it";
  String CLEAR_SCALING_POLICY = "Clearing scaling policy for scale set: [%s]";
  String START_BLUE_GREEN_SWAP = "Starting Swap Backend pool step during blue green deployment";
  String END_BLUE_GREEN_SWAP = "Swap backend pool completed successfully";
  String DOWNSIZING_FLAG_DISABLED = "Skipping downsizing of VMSS: [%s] as downsize was not requested";
  String NO_VMSS_FOR_UPSCALE_DURING_ROLLBACK = "There is no old Virtual machine for up scaling during rollback";

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
      "Parameter newVirtualMachineScaleSetName is required and cannot be null.";
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

  // Patterns
  String GALLERY_IMAGE_ID_PATTERN =
      "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/galleries/%s/images/%s/versions/%s";
  String VMSS_AUTOSCALE_SUFIX = "_Autoscale";

  // VM provisioning statuses
  String VM_PROVISIONING_SUCCEEDED_STATUS = "Provisioning succeeded";
  String VM_PROVISIONING_SPECIALIZED_STATUS = "VM specialized";

  // VM power statuses
  String VM_POWER_STATE_PREFIX = "PowerState/";

  // Azure App Service
  String COMMAND_TYPE_BLANK_VALIDATION_MSG = "Parameter commandType is required and cannot be empty or null";
  String WEB_APP_NAME_BLANK_VALIDATION_MSG = "Parameter webAppName is required and cannot be empty or null";
  String SLOT_NAME_BLANK_VALIDATION_MSG = "Parameter slotName is required and cannot be empty or null";
  String ACR_USERNAME_BLANK_VALIDATION_MSG = "Parameter username cannot be null or empty";
  String ACR_ACCESS_KEYS_BLANK_VALIDATION_MSG = "Primary and secondary ACR access keys cannot be null or empty";
  String DOCKER_REGISTRY_URL_BLANK_VALIDATION_MSG = "Parameter dockerRegistryUrl cannot be empty or null";
  String ACR_REGISTRY_NAME_BLANK_VALIDATION_MSG = "Parameter registryName cannot be empty or null";
  String DEPLOYMENT_SLOT_PRODUCTION_NAME = "production";
  String DOCKER_REGISTRY_SERVER_URL_PROPERTY_NAME = "DOCKER_REGISTRY_SERVER_URL";
  String DOCKER_REGISTRY_SERVER_USERNAME_PROPERTY_NAME = "DOCKER_REGISTRY_SERVER_USERNAME";
  String DOCKER_REGISTRY_SERVER_SECRET_PROPERTY_NAME = "DOCKER_REGISTRY_SERVER_PASSWORD";
  String DOCKER_CUSTOM_IMAGE_NAME_PROPERTY_NAME = "DOCKER_CUSTOM_IMAGE_NAME";
  String DOCKER_IMAGE_FULL_PATH_PATTERN = "DOCKER|%s";
  String DOCKER_FX_IMAGE_PREFIX = "DOCKER|";
  String DOCKER_IMAGE_AND_TAG_PATH_PATTERN = "%s:%s";
  String WEB_APP_NAME_BLANK_ERROR_MSG = "Parameter webAppName cannot be null or empty";
  String SLOT_NAME_BLANK_ERROR_MSG = "Parameter slotName cannot be null or empty";
  String IMAGE_AND_TAG_BLANK_ERROR_MSG = "Parameter imageAndTag cannot be null or empty";
  String SHIFT_TRAFFIC_SLOT_NAME_BLANK_ERROR_MSG = "Parameter shiftTrafficSlotName cannot be null or empty";
  String TRAFFIC_WEIGHT_IN_PERCENTAGE_INVALID_ERROR_MSG =
      "Parameter trafficWeightInPercentage cannot be less then 0% or higher then 100%";
  String SOURCE_SLOT_NAME_BLANK_ERROR_MSG = "Parameter sourceSlotName cannot be null or empty";
  String TARGET_SLOT_NAME_BLANK_ERROR_MSG = "Parameter targetSlotName cannot be null or empty";
  String ACTIVITY_LOG_EVENT_DATA_TEMPLATE = "Operation name : [%s]%n"
      + "Event initiated by : [%s]%n"
      + "Status : [%s]%n"
      + "Description : [%s]";
  String SLOT_SWAP_JOB_PROCESSOR_STR = "SlotSwapJobProcessor";
  String SUCCESS_REQUEST = "Request sent successfully";
  String DEPLOYMENT_SLOT_FULL_NAME_PATTERN = "%s-%s";
  String DEPLOYMENT_SLOT_NAME_PREFIX_PATTERN = "%s-";
  String DEPLOYMENT_SLOT_NON_PRODUCTION_TYPE = "non-production";
  String DEPLOYMENT_SLOT_PRODUCTION_TYPE = "production";

  // Azure App Service Command Units
  String STOP_DEPLOYMENT_SLOT = "Stop Slot";
  String UPDATE_DEPLOYMENT_SLOT_CONFIGURATION_SETTINGS = "Update Slot Configuration Settings";
  String UPDATE_DEPLOYMENT_SLOT_CONTAINER_SETTINGS = "Update Slot Container Settings";
  String START_DEPLOYMENT_SLOT = "Start Slot";
  String SLOT_TRAFFIC_WEIGHT = "Update Slot Traffic Weight";
  String SLOT_SWAP = "Swap Slots";
  long SLOT_STARTING_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);
  long SLOT_STOPPING_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);

  // Azure Docker Registry Type
  String ACR = "ACR";
  String DOCKER_HUB_PRIVATE = "DOCKER_HUB_PRIVATE";
  String DOCKER_HUB_PUBLIC = "DOCKER_HUB_PUBLIC";
  String ARTIFACTORY_PRIVATE_REGISTRY = "ARTIFACTORY_PRIVATE_REGISTRY";

  // Web App Instance status
  String WEB_APP_INSTANCE_STATUS_RUNNING = "Running";

  // App Service Manifest Utils
  Pattern IS_SETTING_SECRET_REGEX = Pattern.compile("^\\$\\{secrets\\.getValue\\(['\"]+(?<secretName>\\S+)['\"]+\\)}$");
}
