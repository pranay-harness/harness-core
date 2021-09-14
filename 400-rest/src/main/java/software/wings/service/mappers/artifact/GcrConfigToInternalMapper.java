/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.mappers.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.gcr.beans.GcrInternalConfig;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class GcrConfigToInternalMapper {
  public GcrInternalConfig toGcpInternalConfig(String gcrHostName, String basicAuthHeader) {
    return GcrInternalConfig.builder().basicAuthHeader(basicAuthHeader).registryHostname(gcrHostName).build();
  }
}
