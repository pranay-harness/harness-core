package software.wings.beans.artifact;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.SearchFilter;
import software.wings.dl.PageRequest;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.Optional;

/**
 * Created by anubhaw on 2/2/17.
 */
@Singleton
public class ArtifactEnumDataProvider implements DataProvider {
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public Map<String, String> getData(String appId, String... params) {
    String serviceId = params[0];
    Optional<ArtifactStream> artifactStream =
        artifactStreamService
            .list(
                PageRequest.Builder.aPageRequest().addFilter("serviceId", SearchFilter.Operator.EQ, serviceId).build())
            .getResponse()
            .stream()
            .findFirst();
    String artifactName = "";
    if (artifactStream.isPresent()) {
      artifactName = artifactStream.get().getSourceName();
    }
    return ImmutableMap.of(artifactName, artifactName);
  }
}
