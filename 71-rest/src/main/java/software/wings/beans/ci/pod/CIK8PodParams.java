package software.wings.beans.ci.pod;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.wings.beans.GitFetchFilesConfig;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CIK8PodParams<T extends ContainerParams> extends PodParams<T> {
  private GitFetchFilesConfig gitFetchFilesConfig;
  private String stepExecVolumeName;
  private String stepExecWorkingDir;

  @Builder
  public CIK8PodParams(GitFetchFilesConfig gitFetchFilesConfig, String stepExecVolumeName, String stepExecWorkingDir,
      String name, String namespace, Map<String, String> labels, List<T> containerParamsList,
      List<T> initContainerParamsList, List<PVCParams> pvcParamList) {
    super(name, namespace, labels, containerParamsList, initContainerParamsList, pvcParamList);
    this.gitFetchFilesConfig = gitFetchFilesConfig;
    this.stepExecVolumeName = stepExecVolumeName;
    this.stepExecWorkingDir = stepExecWorkingDir;
  }

  @Override
  public Type getType() {
    return Type.K8;
  }
}