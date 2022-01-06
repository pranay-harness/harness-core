/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert;

import io.harness.alert.AlertData;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 11/13/17.
 */
@Data
@Builder
public class KmsSetupAlert implements AlertData {
  private String kmsId;
  private String message;

  @Override
  public boolean matches(AlertData alertData) {
    return kmsId.equals(((KmsSetupAlert) alertData).kmsId);
  }

  @Override
  public String buildTitle() {
    return message;
  }

  @Override
  public String buildResolutionTitle() {
    return "Incident Resolved (" + message + ")";
  }
}
