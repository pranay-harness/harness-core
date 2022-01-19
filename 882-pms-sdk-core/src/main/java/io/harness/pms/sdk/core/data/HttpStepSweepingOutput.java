/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@OwnedBy(CDC)
@TypeAlias("k8sCanaryDeleteOutcome")
@JsonTypeName("k8sCanaryDeleteOutcome")
@RecasterAlias("io.harness.cdng.k8s.K8sCanaryDeleteOutcome")
public class HttpStepSweepingOutput implements ExecutionSweepingOutput {
  private String httpResponseBody;
  private int httpResponseCode;
}
