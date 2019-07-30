package io.harness.spotinst.model;

public interface SpotInstConstants {
  String spotInstBaseUrl = "https://api.spotinst.io/";
  String spotInstContentType = "application/json";
  int listElastiGroupsQueryTime = 365;
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
  String CAPACITY = "capacity";
  String UNIT_INSTANCE = "instance";
  String PHASE_PARAM = "PHASE_PARAM";
}