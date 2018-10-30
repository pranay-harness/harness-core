package software.wings.service.intfc.yaml;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.yaml.GitFileChange;
import software.wings.security.AppPermissionSummary;
import software.wings.security.UserPermissionInfo;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.directory.DirectoryNode;
import software.wings.yaml.directory.DirectoryPath;
import software.wings.yaml.directory.FolderNode;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yaml Directory Service.
 *
 * @author bsollish
 */
public interface YamlDirectoryService {
  /**
   * Find by account id.
   *
   * @param accountId the account id
   * @return the directory node (the full account setup/"config-as-code" directory)
   */
  DirectoryNode getDirectory(@NotEmpty String accountId, String appId);

  /**
   * Get Directory (tree/sub-tree structure) by entityId, optionally filtered by nodes ("branches") that have custom git
   * sync
   *
   * @param accountId the account id
   * @param entityId the entity id
   * @param applyPermissions
   * @param userPermissionInfo user permission info
   * @return the directory node (top of the requested "tree")
   */
  FolderNode getDirectory(
      @NotEmpty String accountId, String entityId, boolean applyPermissions, UserPermissionInfo userPermissionInfo);

  YamlGitConfig weNeedToPushChanges(String accountId, String entityId);

  List<GitFileChange> traverseDirectory(List<GitFileChange> gitFileChanges, String accountId, FolderNode fn,
      String path, boolean includeFiles, boolean failFast, Optional<List<String>> listOfYamlErrors);

  String getRootPath();

  String getRootPathByApp(Application app);

  String getRootPathByService(Service service);

  String getRootPathByService(Service service, String applicationPath);

  String getRootPathByServiceCommand(Service service, ServiceCommand serviceCommand);

  String getRootPathByContainerTask(Service service, ContainerTask containerTask);

  String getRootPathByHelmChartSpecification(Service service, HelmChartSpecification helmChartSpecification);

  String getRootPathByLambdaSpec(Service service, LambdaSpecification lambdaSpecification);

  String getRootPathByUserDataSpec(Service service, UserDataSpecification userDataSpecification);

  <T> String getRootPathByConfigFile(T entity);

  String getRootPathByConfigFileOverride(Environment environment);

  String getRootPathByEnvironment(Environment environment);

  String getRootPathByEnvironment(Environment environment, String appName);

  String getRootPathByInfraMapping(InfrastructureMapping infraMapping);

  String getRootPathByPipeline(Pipeline pipeline);

  String getRootPathByWorkflow(Workflow workflow);

  String getRootPathByArtifactStream(ArtifactStream artifactStream);

  String getRootPathBySettingAttribute(SettingAttribute settingAttribute, SettingVariableTypes settingVariableType);

  String getRootPathByNotificationGroup(NotificationGroup notificationGroup);

  String getRootPathBySettingAttribute(SettingAttribute settingAttribute);

  String getRootPathByPcfServiceSpecification(Service service, PcfServiceSpecification pcfServiceSpecification);

  String getRootPathByInfraProvisioner(InfrastructureProvisioner provisioner);

  void getGitFileChange(DirectoryNode dn, String path, String accountId, boolean includeFiles,
      List<GitFileChange> gitFileChanges, boolean failFast, Optional<List<String>> listOfYamlErrors,
      boolean gitSyncPath);

  <R, T> String obtainEntityRootPath(R helperEntity, T entity);

  DirectoryNode getApplicationYamlFolderNode(@NotEmpty String accountId, @NotEmpty String applicationId);

  FolderNode doApplication(String applicationId, boolean applyPermissions,
      Map<String, AppPermissionSummary> appPermissionSummaryMap, FolderNode applicationsFolder,
      DirectoryPath directoryPath);
}
