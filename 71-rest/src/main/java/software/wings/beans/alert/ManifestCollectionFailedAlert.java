package software.wings.beans.alert;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;

@OwnedBy(CDC)
@Data
@Builder
public class ManifestCollectionFailedAlert implements AlertData {
  @Inject ApplicationManifestService applicationManifestService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject AppService appService;
  @Inject SettingsService settingsService;

  private String appId;
  private String serviceId;
  private String appManifestId;

  @Override
  public boolean matches(AlertData alertData) {
    ArtifactCollectionFailedAlert otherAlert = (ArtifactCollectionFailedAlert) alertData;
    return StringUtils.equals(otherAlert.getArtifactStreamId(), appManifestId);
  }

  @Override
  public String buildTitle() {
    StringBuilder title = new StringBuilder(128);
    title.append("Manifest collection ");
    ApplicationManifest applicationManifest = applicationManifestService.getById(appId, appManifestId);
    if (applicationManifest != null) {
      if (isNotBlank(appId)) {
        if (isNotBlank(serviceId)) {
          Service service = serviceResourceService.getWithDetails(appId, serviceId);
          title.append("for manifest in service ").append(service.getName()).append(' ');
        }
        Application app = appService.get(appId);
        title.append("for application ").append(app.getName()).append(' ');
      }
    }

    title.append("is disabled because of multiple failed attempts. Fix manifest setup or do a manual pull");
    return title.toString();
  }
}
