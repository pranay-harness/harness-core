package software.wings.beans.ci.pod;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.wings.beans.container.ImageDetails;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CIK8ContainerParams extends ContainerParams {
  private CIContainerType containerType;

  @Builder
  public CIK8ContainerParams(CIContainerType containerType, String name, ImageDetails imageDetails,
      List<String> commands, List<String> args, Map<String, String> envVars, Map<String, String> volumeToMountPath,
      ContainerResourceParams containerResourceParams) {
    super(name, imageDetails, commands, args, envVars, volumeToMountPath, containerResourceParams);
    this.containerType = containerType;
  }

  @Override
  public ContainerParams.Type getType() {
    return ContainerParams.Type.K8;
  }
}