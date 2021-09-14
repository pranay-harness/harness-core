/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.citasks.cik8handler.k8java.pod;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.pod.ContainerParams;
import io.harness.delegate.beans.ci.pod.PodParams;
import io.harness.delegate.task.citasks.cik8handler.params.CIConstants;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import io.kubernetes.client.openapi.models.V1PodBuilder;
import io.kubernetes.client.openapi.models.V1PodFluent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CI)
public class PodSpecBuilder extends BasePodSpecBuilder {
  @Override
  protected void decorateSpec(
      PodParams<ContainerParams> podParams, V1PodFluent.SpecNested<V1PodBuilder> podBuilderSpecNested) {
    if (podParams.getType() != PodParams.Type.K8) {
      log.error("Type mismatch: pod parameters is not of type: {}", PodParams.Type.K8);
      throw new InvalidRequestException("Type miss matched");
    }

    podBuilderSpecNested.withRestartPolicy(CIConstants.RESTART_POLICY);
    podBuilderSpecNested.withActiveDeadlineSeconds(CIConstants.POD_MAX_TTL_SECS);
  }
}
