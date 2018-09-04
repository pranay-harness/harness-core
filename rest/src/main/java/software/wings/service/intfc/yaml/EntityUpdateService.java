package software.wings.service.intfc.yaml;

import software.wings.beans.ConfigFile;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;

import java.util.List;

/**
 * Entity Update Service.
 *
 * @author bsollish
 */
public interface EntityUpdateService {
  GitFileChange getServiceGitSyncFile(String accountId, Service service, ChangeType changeType);

  GitFileChange getDefaultVarGitSyncFile(String accountId, String appId, ChangeType changeType);

  GitFileChange getCommandGitSyncFile(
      String accountId, Service service, ServiceCommand serviceCommand, ChangeType changeType);

  List<GitFileChange> getConfigFileGitSyncFileSet(
      String accountId, Service service, ConfigFile configFile, ChangeType changeType, String fileContent);

  List<GitFileChange> getConfigFileOverrideGitSyncFileSet(
      String accountId, Environment environment, ConfigFile configFile, ChangeType changeType, String fileContent);

  List<GitFileChange> getEnvironmentGitSyncFile(String accountId, Environment environment, ChangeType changeType);

  GitFileChange getSettingAttributeGitSyncFile(
      String accountId, SettingAttribute settingAttribute, ChangeType changeType);

  GitFileChange getInfraProvisionerGitSyncFile(
      String accountId, InfrastructureProvisioner provisioner, ChangeType changeType);

  <T> List<GitFileChange> obtainEntityGitSyncFileChangeSet(
      String accountId, Service service, T entity, ChangeType changeType);
}
