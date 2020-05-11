package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.EntityType;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.stencils.DataProvider;

import java.util.Map;
import java.util.Optional;

@OwnedBy(CDC)
@Singleton
public class ArtifactEnumDataProvider implements DataProvider {
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    Optional<ArtifactStream> artifactStream =
        artifactStreamServiceBindingService.listArtifactStreams(appId, params.get(EntityType.SERVICE.name()))
            .stream()
            .findFirst();
    String artifactName = artifactStream.isPresent() ? artifactStream.get().getSourceName() : "";
    return ImmutableMap.of(artifactName, artifactName);
  }
}
