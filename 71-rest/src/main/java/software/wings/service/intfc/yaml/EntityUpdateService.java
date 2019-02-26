package software.wings.service.intfc.yaml;

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
  List<GitFileChange> getDefaultVarGitSyncFile(String accountId, String appId, ChangeType changeType);

  GitFileChange getCommandGitSyncFile(
      String accountId, Service service, ServiceCommand serviceCommand, ChangeType changeType);

  <R, T> List<GitFileChange> obtainEntityGitSyncFileChangeSet(
      String accountId, R helperEntity, T entity, ChangeType changeType);

  List<GitFileChange> obtainSettingAttributeRenameChangeSet(
      String accountId, SettingAttribute oldSettingAttribute, SettingAttribute newSettingAttribute);

  List<GitFileChange> obtainDefaultVariableChangeSet(String accountId, String appId, ChangeType changeType);

  <T> String obtainAppIdFromEntity(T entity);

  <T> String obtainAccountIdFromEntity(T entity);

  <R, T> String getEntityRootFilePath(T entity);
}