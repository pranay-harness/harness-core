/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConnectorValidationParameterResponse {
  ConnectorValidationParams connectorValidationParams;
  @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) Boolean isInvalid;

  public void setInvalid(boolean isInvalid) {
    this.isInvalid = isInvalid;
  }

  public boolean isInvalid() {
    return Boolean.TRUE.equals(isInvalid);
  }
}
