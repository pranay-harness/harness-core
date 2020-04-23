package software.wings.beans.ci.pod;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.container.ImageDetails;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public abstract class ContainerParams {
  private String name;
  private ImageDetails imageDetails;
  private List<String> commands;
  private List<String> args;
  private Map<String, String> envVars;
  private Map<String, String> volumeToMountPath;
  private ContainerResourceParams containerResourceParams;

  public abstract ContainerParams.Type getType();

  public enum Type {
    K8, // Generic K8 container configuration
    K8_GIT_CLONE, // K8 container configuration to clone a git repository
  }
}