package software.wings.service.intfc;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.service.intfc.ownership.OwnedByService;
import software.wings.yaml.directory.DirectoryNode;

import java.util.List;

public interface ApplicationManifestService extends OwnedByService {
  ApplicationManifest create(ApplicationManifest applicationManifest);

  ManifestFile createManifestFile(ManifestFile manifestFile, String serviceId);

  ManifestFile updateManifestFile(ManifestFile manifestFile, String serviceId);

  ApplicationManifest update(ApplicationManifest applicationManifest);

  ApplicationManifest getByServiceId(String appId, String serviceId);

  ApplicationManifest getById(String appId, String id);

  List<ManifestFile> getManifestFiles(String appId, String serviceId);

  List<ManifestFile> getManifestFilesByAppManifestId(String appId, String applicationManifestId);

  ManifestFile getManifestFileById(String appId, String id);

  void deleteManifestFiles(String appId, String applicationManifestId);

  ManifestFile getManifestFileByFileName(String applicationManifestId, String fileName);

  ManifestFile upsertApplicationManifestFile(ManifestFile manifestFile, String serviceId, boolean isCreate);

  void deleteManifestFileById(String appId, String manifestFileId);

  void deleteAppManifest(String appId, String appManifestId);

  DirectoryNode getManifestFilesFromGit(String appId, String appManifestId);
}