/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.ci.k8s;

import io.harness.delegate.beans.ci.ExecuteCommandTaskParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class K8ExecuteCommandTaskParams implements ExecuteCommandTaskParams {
  @NotNull private ConnectorDetails k8sConnector;
  @NotNull private K8ExecCommandParams k8ExecCommandParams;
  @Builder.Default private static final Type type = Type.GCP_K8;

  @Override
  public Type getType() {
    return type;
  }
}
