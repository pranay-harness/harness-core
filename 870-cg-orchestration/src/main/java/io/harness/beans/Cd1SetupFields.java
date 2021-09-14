/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Cd1SetupFields {
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String APP_ID_FIELD = "appId";
  public static final String ENV_ID_FIELD = "envId";
  public static final String INFRASTRUCTURE_MAPPING_ID_FIELD = "infrastructureMappingId";
  public static final String SERVICE_TEMPLATE_ID_FIELD = "serviceTemplateId";
  public static final String ARTIFACT_STREAM_ID_FIELD = "artifactStreamId";
  public static final String ENV_TYPE_FIELD = "envType";
  public static final String SERVICE_ID_FIELD = "serviceId";

  public static final String APPLICATION = "Application";
  public static final String SERVICE = "Service";
  public static final String ENVIRONMENT_TYPE = "Environment Type";
  public static final String ENVIRONMENT = "Environment";

  public static String mapSetupFieldKeyToHumanFriendlyName(String fieldKey) {
    switch (fieldKey) {
      case APP_ID_FIELD:
        return APPLICATION;
      case SERVICE_ID_FIELD:
        return SERVICE;
      case ENV_TYPE_FIELD:
        return ENVIRONMENT_TYPE;
      case ENV_ID_FIELD:
        return ENVIRONMENT;
      default:
        return fieldKey;
    }
  }
}
