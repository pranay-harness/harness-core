/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorConstants {
  public static final String CONNECTOR_DECORATOR_SERVICE = "connectorDecoratorService";
  public static final String CONNECTIVITY_STATUS = "connectivityStatus";
  public static final String CONNECTOR_IDENTIFIER = "connectorIdentifier";
  public static final String CONNECTOR_TYPES = "type";
  public static final String INHERIT_FROM_DELEGATE_TYPE_ERROR_MSG =
      "Delegate Selector cannot be null for inherit from delegate credential type";
}
