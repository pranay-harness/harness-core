package io.harness.ccm.health;

import lombok.Getter;

public enum CEConnectorHealthMessages {
  SETTING_ATTRIBUTE_CREATED("Cloud Account Connector has been setup correctly"),

  BILLING_PIPELINE_CREATION_SUCCESSFUL("Billing Data Pipeline was created successfully"),

  BILLING_PIPELINE_CREATION_FAILED("Error Creating Billing Data Pipeline"),

  BILLING_DATA_PIPELINE_ERROR("Error Processing Data"),

  BILLING_DATA_PIPELINE_SUCCESS("The data is being processed actively"),

  WAITING_FOR_SUCCESSFUL_AWS_S3_SYNC_MESSAGE("Processing CUR Data Sync"),

  AWS_S3_SYNC_MESSAGE("Last Successful S3 Sync at %s");

  @Getter private String message;

  CEConnectorHealthMessages(String message) {
    this.message = message;
  }
}
