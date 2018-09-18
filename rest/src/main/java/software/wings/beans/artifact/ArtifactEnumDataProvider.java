package software.wings.beans.artifact;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.EntityType;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.Optional;

@Singleton
public class ArtifactEnumDataProvider implements DataProvider {
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    Optional<ArtifactStream> artifactStream =
        artifactStreamService
            .list(aPageRequest()
                      .addFilter("serviceId", EQ, params.get(EntityType.SERVICE.name()))
                      .addFilter("appId", EQ, appId)
                      .build())
            .getResponse()
            .stream()
            .findFirst();
    String artifactName = artifactStream.isPresent() ? artifactStream.get().getSourceName() : "";
    return ImmutableMap.of(artifactName, artifactName);
  }
}
