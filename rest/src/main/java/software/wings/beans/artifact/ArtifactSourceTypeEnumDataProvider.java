package software.wings.beans.artifact;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.stencils.DataProvider;
import software.wings.utils.ArtifactType;

import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 2/2/17.
 */
@Singleton
public class ArtifactSourceTypeEnumDataProvider implements DataProvider {
  @SuppressWarnings("unused") @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    String serviceId = params[0];
    Service service = serviceResourceService.get(appId, serviceId);
    if (service.getArtifactType().equals(ArtifactType.DOCKER)) {
      return ImmutableMap.of(ArtifactStreamType.DOCKER.name(), ArtifactStreamType.DOCKER.name(),
          ArtifactStreamType.ECR.name(), ArtifactStreamType.ECR.name(), ArtifactStreamType.GCR.name(),
          ArtifactStreamType.GCR.name());
    } else {
      return ImmutableMap.of(ArtifactStreamType.JENKINS.name(), ArtifactStreamType.JENKINS.name(),
          ArtifactStreamType.BAMBOO.name(), ArtifactStreamType.BAMBOO.name());
    }
  }
}
