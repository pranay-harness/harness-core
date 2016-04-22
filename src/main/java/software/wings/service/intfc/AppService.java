package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.InputStream;
import java.util.List;

import java.util.List;

/**
 * Application Service.
 *
 * @author Rishi
 */
public interface AppService {
  Application save(Application app);

  List<Application> list();

  PageResponse<Application> list(PageRequest<Application> req);

  Application findByUUID(String uuid);

  Application update(Application app);

  String savePlatformSoftware(
      PlatformSoftware platformSoftware, InputStream uploadedInputStream, FileBucket fileBucket);

  String updatePlatformSoftware(
      String platformID, PlatformSoftware platformSoftware, InputStream uploadedInputStream, FileBucket softwares);

  List<PlatformSoftware> getPlatforms(String appID);

  PlatformSoftware getPlatform(String appID, String platformID);

  void deletePlatform(String appID, String platformID);

  void deleteApp(String appID);
}
