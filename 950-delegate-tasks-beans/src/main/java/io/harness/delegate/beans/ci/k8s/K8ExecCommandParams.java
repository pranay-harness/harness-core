/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.ci.k8s;

import io.harness.delegate.beans.ci.ShellScriptType;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder
public class K8ExecCommandParams {
  @NotNull private String podName;
  @NotNull private String namespace;
  @NotNull private String containerName;
  @NotNull private List<String> commands;
  @NotNull private ShellScriptType scriptType;
  @NotNull private String mountPath;
  @NotNull private String relStderrFilePath; // Stderr file path relative to mount path
  @NotNull private String relStdoutFilePath; // Stdout file path relative to mount path
  @NotNull private Integer commandTimeoutSecs;
  @NotNull private ConnectorDetails k8Connector;
}
