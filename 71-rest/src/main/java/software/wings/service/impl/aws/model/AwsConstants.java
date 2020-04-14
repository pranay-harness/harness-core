package software.wings.service.impl.aws.model;

public interface AwsConstants {
  String AWS_SIMPLE_HTTP_CONNECTIVITY_URL = "https://aws.amazon.com/";
  int DEFAULT_AMI_ASG_MAX_INSTANCES = 10;
  int DEFAULT_AMI_ASG_MIN_INSTANCES = 0;
  int DEFAULT_AMI_ASG_DESIRED_INSTANCES = 6;
  int DEFAULT_AMI_ASG_TIMEOUT_MIN = 10;
  String DEFAULT_AMI_ASG_NAME = "DEFAULT_ASG";
  String AMI_SETUP_COMMAND_NAME = "AMI Service Setup";
  String AWS_AMI_ALL_PHASE_ROLLBACK_NAME = "Aws Ami All Phase Rollback";
  String PHASE_PARAM = "PHASE_PARAM";
  String FORWARD_LISTENER_ACTION = "forward";

  // Alb Traffic Shift Constants
  int MAX_TRAFFIC_SHIFT_WEIGHT = 100;
  int MIN_TRAFFIC_SHIFT_WEIGHT = 0;

  // Main Ecs Container Name
  String MAIN_ECS_CONTAINER_NAME_TAG = "HARNESS_DEPLOYED_MAIN_CONTAINER";
}