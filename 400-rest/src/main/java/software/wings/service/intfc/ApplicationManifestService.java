/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.AppManifestSource;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.yaml.directory.DirectoryNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ApplicationManifestService extends OwnedByService, OwnedByEnvironment {
  ApplicationManifest create(ApplicationManifest applicationManifest);

  ManifestFile createManifestFileByServiceId(ManifestFile manifestFile, String serviceId);

  ManifestFile createManifestFileByServiceId(ManifestFile manifestFile, String serviceId, boolean removeNamespace);

  ManifestFile createManifestFileByServiceId(ManifestFile manifestFile, String serviceId, AppManifestKind kind);

  ManifestFile updateManifestFileByServiceId(ManifestFile manifestFile, String serviceId);

  ManifestFile updateManifestFileByServiceId(ManifestFile manifestFile, String serviceId, boolean removeNamespace);

  ApplicationManifest update(ApplicationManifest applicationManifest);

  @Deprecated ApplicationManifest getManifestByServiceId(String appId, String serviceId);

  ApplicationManifest getById(String appId, String id);

  List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId);

  ManifestFile getManifestFileById(String appId, String id);

  ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName);

  ManifestFile upsertApplicationManifestFile(
      ManifestFile manifestFile, ApplicationManifest applicationManifest, boolean isCreate);

  void deleteManifestFileById(String appId, String manifestFileId);

  void deleteAppManifest(String appId, String appManifestId);

  DirectoryNode getManifestFilesFromGit(String appId, String appManifestId);

  List<ApplicationManifest> getManifestsByServiceId(String appId, String serviceId, AppManifestKind kind);

  ApplicationManifest getByEnvId(String appId, String envId, AppManifestKind kind);

  ApplicationManifest getByEnvAndServiceId(String appId, String envId, String serviceId, AppManifestKind kind);

  ApplicationManifest getAppManifest(String appId, String envId, String serviceId, AppManifestKind kind);

  @Nonnull AppManifestSource getAppManifestType(@Nonnull ApplicationManifest applicationManifest);

  List<ApplicationManifest> getAllByEnvId(String appId, String envId);

  List<ApplicationManifest> getAllByConnectorId(String accountId, String connectorId, Set<StoreType> storeTypes);

  void deleteManifestFile(String appId, ManifestFile manifestFile);

  @Override void pruneByEnvironment(String appId, String envId);

  void deleteAppManifest(ApplicationManifest applicationManifest);

  List<ApplicationManifest> getAllByEnvIdAndKind(String appId, String envId, AppManifestKind kind);

  ManifestFile getManifestFileByEnvId(String appId, String envId, AppManifestKind kind);

  List<ApplicationManifest> listAppManifests(String appId, String serviceId);

  List<ManifestFile> listManifestFiles(String appManifestId, String appId);

  @Deprecated ApplicationManifest getByServiceId(String appId, String serviceId, AppManifestKind kind);

  void cloneManifestFiles(
      String appId, ApplicationManifest applicationManifestOld, ApplicationManifest applicationManifestNew);

  void deleteAllManifestFilesByAppManifestId(String appId, String appManifestId);

  PageResponse<ApplicationManifest> list(PageRequest<ApplicationManifest> pageRequest);

  PageResponse<ApplicationManifest> listPollingEnabled(PageRequest<ApplicationManifest> pageRequest, String appId);

  List<ManifestFile> getOverrideManifestFilesByEnvId(String appId, String envId);

  boolean detachPerpetualTask(@NotNull String perpetualTaskId, String accountId);

  boolean attachPerpetualTask(String accountId, @NotNull String appManifestId, @NotNull String perpetualTaskId);

  Map<String, String> fetchAppManifestProperties(String appId, String applicationManifestId);

  boolean updateFailedAttempts(String accountId, String appManifestId, int failedAttempts);

  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String applicationManifestId);

  ApplicationManifest getAppManifestByName(String appId, String envId, String serviceId, String appManifestName);

  Map<String, String> getNamesForIds(String appId, Set<String> appManifestIds);

  boolean deletePerpetualTaskByAppManifest(String accountId, String appManifestId);
}
