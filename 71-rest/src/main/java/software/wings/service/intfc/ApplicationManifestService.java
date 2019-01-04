package software.wings.service.intfc;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.AppManifestType;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.yaml.directory.DirectoryNode;

import java.util.List;

public interface ApplicationManifestService extends OwnedByService, OwnedByEnvironment {
  ApplicationManifest create(ApplicationManifest applicationManifest);

  ManifestFile createManifestFileByServiceId(ManifestFile manifestFile, String serviceId);

  ManifestFile updateManifestFileByServiceId(ManifestFile manifestFile, String serviceId);

  ApplicationManifest update(ApplicationManifest applicationManifest);

  ApplicationManifest getByServiceId(String appId, String serviceId);

  ApplicationManifest getById(String appId, String id);

  List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId);

  ManifestFile getManifestFileById(String appId, String id);

  ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName);

  ManifestFile upsertApplicationManifestFile(
      ManifestFile manifestFile, ApplicationManifest applicationManifest, boolean isCreate);

  void deleteManifestFileById(String appId, String manifestFileId);

  void deleteAppManifest(String appId, String appManifestId);

  DirectoryNode getManifestFilesFromGit(String appId, String appManifestId);

  ApplicationManifest getByEnvId(String appId, String envId);

  ApplicationManifest getByEnvAndServiceId(String appId, String envId, String serviceId);

  ApplicationManifest getAppManifest(String appId, String envId, String serviceId);

  AppManifestType getAppManifestType(ApplicationManifest applicationManifest);

  List<ApplicationManifest> getAllByEnvId(String appId, String envId);

  void deleteManifestFile(String appId, ManifestFile manifestFile);

  void pruneByEnvironment(String appId, String envId);

  void deleteAppManifest(ApplicationManifest applicationManifest);
}