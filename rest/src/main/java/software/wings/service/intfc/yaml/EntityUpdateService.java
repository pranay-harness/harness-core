package software.wings.service.intfc.yaml;

import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;

/**
 * Entity Update Service.
 *
 * @author bsollish
 */
public interface EntityUpdateService {
  GitFileChange getAppGitSyncFile(Application app, ChangeType changeType);

  GitFileChange getServiceGitSyncFile(String accountId, Service service, ChangeType changeType);

  GitFileChange getCommandGitSyncFile(
      String accountId, Service service, ServiceCommand serviceCommand, ChangeType changeType);

  GitFileChange getEnvironmentGitSyncFile(String accountId, Environment environment, ChangeType changeType);

  GitFileChange getInfraMappingGitSyncFile(String accountId, InfrastructureMapping infraMapping, ChangeType changeType);

  GitFileChange getWorkflowGitSyncFile(String accountId, Workflow workflow, ChangeType changeType);

  GitFileChange getPipelineGitSyncFile(String accountId, Pipeline pipeline, ChangeType changeType);

  GitFileChange getArtifactStreamGitSyncFile(String accountId, ArtifactStream ArtifactStream, ChangeType changeType);

  GitFileChange getSettingAttributeGitSyncFile(
      String accountId, SettingAttribute settingAttribute, ChangeType changeType);
}
