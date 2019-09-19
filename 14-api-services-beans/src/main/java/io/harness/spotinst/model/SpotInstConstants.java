package io.harness.spotinst.model;

public interface SpotInstConstants {
  String spotInstBaseUrl = "https://api.spotinst.io/";
  int listElastiGroupsQueryTime = 365;
  int elastiGroupsToKeep = 3;
  int defaultSteadyStateTimeout = 15;
  int defaultSyncSpotinstTimeoutMin = 2;
  String ELASTI_GROUP_NAME_PLACEHOLDER = "${ELASTI_GROUP_NAME}";
  String NAME_CONFIG_ELEMENT = "name";
  String CAPACITY_UNIT_CONFIG_ELEMENT = "unit";
  String CAPACITY_MINIMUM_CONFIG_ELEMENT = "minimum";
  String CAPACITY_TARGET_CONFIG_ELEMENT = "target";
  String CAPACITY_MAXIMUM_CONFIG_ELEMENT = "maximum";
  String TG_NAME_PLACEHOLDER = "${TARGET_GROUP_NAME}";
  String TG_ARN_PLACEHOLDER = "${TARGET_GROUP_ARN}";
  String LB_TYPE_TG = "TARGET_GROUP";
  String COMPUTE = "compute";
  String LAUNCH_SPECIFICATION = "launchSpecification";
  String LOAD_BALANCERS_CONFIG = "loadBalancersConfig";
  String LOAD_BALANCERS = "loadBalancers";
  String CAPACITY = "capacity";
  String UNIT_INSTANCE = "instance";
  String PHASE_PARAM = "PHASE_PARAM";
  String STAGE_ELASTI_GROUP_NAME_SUFFIX = "STAGE__Harness";
  String ELASTI_GROUP_IMAGE_CONFIG = "imageId";
  String ELASTI_GROUP_USER_DATA_CONFIG = "userData";
  String GROUP_CONFIG_ELEMENT = "group";

  // Command unit Names
  String SETUP_COMMAND_UNIT = "Setup Elastigroup";
  String UP_SCALE_COMMAND_UNIT = "Upscale Elastigroup";
  String UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT = "Upscale wait for steady state";
  String DOWN_SCALE_COMMAND_UNIT = "Downscale Elastigroup";
  String DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT = "Downscale wait for steady state";
  String SWAP_ROUTES_COMMAND_UNIT = "Swap Routes";
  String RENAME_COMMAND_UNIT = "Rename Elastigroup";
  String RENAME_OLD_COMMAND_UNIT = "Rename old Elastigroup";
  String RENAME_NEW_COMMAND_UNIT = "Rename new Elastigroup";
  String DEPLOYMENT_ERROR = "Final Deployment status";

  String ELASTI_GROUP_ID = "id";
  String ELASTI_GROUP_CREATED_AT = "createdAt";
  String ELASTI_GROUP_UPDATED_AT = "updatedAt";
}