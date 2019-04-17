package software.wings.beans.alert;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;

@Data
@Builder
public class ArtifactCollectionFailedAlert implements AlertData {
  @Inject @Transient @SchemaIgnore private transient AppService appService;
  @Inject @Transient @SchemaIgnore private transient ArtifactStreamService artifactStreamService;
  @Inject @Transient @SchemaIgnore private transient ServiceResourceService serviceResourceService;

  private String appId;
  private String serviceId;
  private String artifactStreamId;

  @Override
  public boolean matches(AlertData alertData) {
    ArtifactCollectionFailedAlert otherAlert = (ArtifactCollectionFailedAlert) alertData;
    return StringUtils.equals(otherAlert.getAppId(), appId)
        && StringUtils.equals(otherAlert.getArtifactStreamId(), artifactStreamId);
  }

  @Override
  public String buildTitle() {
    StringBuilder title = new StringBuilder(128);
    title.append("Artifact collection ");
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    if (artifactStream != null) {
      title.append("of source ").append(artifactStream.getName()).append(' ');
      String serviceId = artifactStream.getServiceId();
      if (isNotBlank(serviceId)) {
        Service service = serviceResourceService.get(appId, artifactStream.getServiceId());
        title.append("in service ").append(service.getName()).append(' ');
      }
      if (isNotBlank(appId) && !appId.equals(GLOBAL_APP_ID)) {
        Application app = appService.get(appId);
        title.append("for application ").append(app.getName()).append(' ');
      }
    }

    title.append("is disabled because of multiple failed attempts. Fix artifact source setup or do a manual pull");
    return title.toString();
  }
}
