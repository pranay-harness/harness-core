package software.wings.yaml.directory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectoryObjectsWrapper {
  private Application app;
  private Service service;
  private ServiceCommand serviceCommand;
  private Environment environment;
  private Workflow workflow;
  private Pipeline pipeline;
  private ArtifactStream artifactStream;
  private SettingAttribute settingAttribute;

  private Class yamlClass;
}
