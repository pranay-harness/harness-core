/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
