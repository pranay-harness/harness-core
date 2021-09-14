/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.container;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KubernetesBlueGreenConfig {
  private KubernetesServiceSpecification primaryService;
  private KubernetesServiceSpecification stageService;
  private boolean useIngress;
  private String ingressYaml;
}
