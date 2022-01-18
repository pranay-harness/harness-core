/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DEL)
public class DelegateTelemetryConstants {
  public static final String DELEGATE_CREATED_EVENT = "Delegate Created";
  public static final String DELEGATE_REGISTERED_EVENT = "Delegate Registered";
  public static final String COUNT_OF_CONNECTED_DELEGATES = "Count of connected delegates";
  public static final String COUNT_OF_REGISTERED_DELEGATES = "Count of registered delegates";
}
