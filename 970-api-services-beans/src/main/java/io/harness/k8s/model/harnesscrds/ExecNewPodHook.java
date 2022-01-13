/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.openapi.models.V1EnvVar;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ExecNewPodHook {
  private List<String> command = new ArrayList();
  private String containerName;
  private List<V1EnvVar> env = new ArrayList();
  private List<String> volumes = new ArrayList();
}
