package io.harness.delegate.beans.ci.pod;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;

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
      String stepExecWorkingDir, String name, String namespace, Map<String, String> labels, List<T> containerParamsList,
      List<T> initContainerParamsList, List<PVCParams> pvcParamList, List<HostAliasParams> hostAliasParamsList) {
    super(name, namespace, labels, containerParamsList, initContainerParamsList, pvcParamList, hostAliasParamsList);
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