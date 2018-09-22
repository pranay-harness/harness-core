package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.stencils.DataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * Created by sgurubelli on 11/15/17.
 */
@Singleton
public class ArtifactSourceProvider implements DataProvider {
  @Inject WingsPersistence wingsPersistence;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    List<ArtifactStream> artifactStreams = new ArrayList<>();
    List<Service> services = new ArrayList<>();
    if (appId != null) {
      artifactStreams = wingsPersistence.createQuery(ArtifactStream.class).filter("appId", appId).asList();
      services = wingsPersistence.createQuery(Service.class).filter("appId", appId).asList();
    }
    Map<String, String> serviceIdToName = services.stream().collect(toMap(Service::getUuid, Service::getName));
    return artifactStreams.stream().collect(
        toMap(o -> o.getUuid(), o -> o.getSourceName() + " (" + serviceIdToName.get(o.getServiceId()) + ")"));
  }
}
