/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CIK8PodParams<T extends ContainerParams> extends PodParams<T> {
  private final ConnectorDetails gitConnector;
  private final String branchName;
  private final String commitId;
  private final String stepExecVolumeName;
  private final String stepExecWorkingDir;

  @Builder
  public CIK8PodParams(ConnectorDetails gitConnector, String branchName, String commitId, String stepExecVolumeName,
      String stepExecWorkingDir, String name, String namespace, Map<String, String> annotations,
      Map<String, String> labels, List<T> containerParamsList, List<T> initContainerParamsList,
      List<PVCParams> pvcParamList, List<HostAliasParams> hostAliasParamsList, Integer runAsUser,
      String serviceAccountName) {
    super(name, namespace, annotations, labels, containerParamsList, initContainerParamsList, pvcParamList,
        hostAliasParamsList, runAsUser, serviceAccountName);
    this.gitConnector = gitConnector;
    this.branchName = branchName;
    this.commitId = commitId;
    this.stepExecVolumeName = stepExecVolumeName;
    this.stepExecWorkingDir = stepExecWorkingDir;
  }

  @Override
  public Type getType() {
    return Type.K8;
  }
}
