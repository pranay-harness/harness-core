/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.health;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;

@OwnedBy(CE)
public enum CEConnectorHealthMessages {
  SETTING_ATTRIBUTE_CREATED("Cloud Account Connector has been setup correctly"),

  BILLING_PIPELINE_CREATION_SUCCESSFUL("Billing Data Pipeline was created successfully"),

  BILLING_PIPELINE_CREATION_FAILED("Error Creating Billing Data Pipeline"),

  BILLING_DATA_PIPELINE_ERROR("Error Processing Data"),

  BILLING_DATA_PIPELINE_SUCCESS("The data is being processed actively"),

  WAITING_FOR_SUCCESSFUL_AWS_S3_SYNC_MESSAGE("Processing CUR Data Sync"),

  AWS_S3_SYNC_MESSAGE("Last Successful S3 Sync at {}"),

  WAITING_FOR_SUCCESSFUL_AZURE_STORAGE_SYNC_MESSAGE("Processing Azure Billing Export Data Sync"),

  AZURE_STORAGE_SYNC_MESSAGE("Last Successful Storage Sync at {}");

  @Getter private String message;

  CEConnectorHealthMessages(String message) {
    this.message = message;
  }
}
