/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.k8s;

public interface K8sTestConstants {
  String VALUES_YAML_WITH_ARTIFACT_REFERENCE = "replicas: 1\n"
      + "image: ${artifact.metadata.image}\n"
      + "dockercfg: ${artifact.source.dockerconfig}";

  String VALUES_YAML_WITH_COMMENTED_ARTIFACT_REFERENCE = "replicas: 1\n"
      + "#image: ${artifact.metadata.image}\n"
      + "  #  dockercfg: ${artifact.source.dockerconfig}";

  String VALUES_YAML_WITH_NO_ARTIFACT_REFERENCE = "replicas: 1";
}
