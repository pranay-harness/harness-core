package io.harness.event.model;

public interface EventConstants {
  String ACCOUNTID = "ACCOUNTID";
  String ACCOUNTNAME = "ACCOUNTNAME";
  String WORKFLOW_EXECUTION_STATUS = "WORKFLOW_EXECUTION_STATUS"; // ABORTED,FAILED,SUCCEEDED etc
  String WORKFLOW_TYPE = "WORKFLOW_TYPE"; // AUTOMATIC / MANUAL
  String WORKFLOW_DURATION = "WORKFLOW_DURATION";
  String AUTOMATIC_WORKFLOW_TYPE = "AUTOMATIC";
  String MANUAL_WORKFLOW_TYPE = "MANUAL";
  String USER_LOGGED_IN = "USER_LOGGED_IN";
  String SETUP_DATA_TYPE = "SETUP_DATA_TYPE";
  String NUMBER_OF_APPLICATIONS = "NUMBER_OF_APPLICATIONS";
  String NUMBER_OF_WORKFLOWS = "NUMBER_OF_WORKFLOWS";
  String NUMBER_OF_ENVIRONMENTS = "NUMBER_OF_ENVIRONMENTS";
  String NUMBER_OF_SERVICES = "NUMBER_OF_SERVICES";
  String NUMBER_OF_PIPELINES = "NUMBER_OF_PIPELINES";
  String NUMBER_OF_TRIGGERS = "NUMBER_OF_TRIGGERS";

  String LOG_ML_FEEDBACKTYPE = "LOG_ML_FEEDBACK_TYPE";
  String VERIFICATION_STATE_TYPE = "VERIFICATION_STATE_TYPE";
  String APPLICATIONID = "APPLICATIONID";
  String WORKFLOWID = "WORKFLOWID";
}
