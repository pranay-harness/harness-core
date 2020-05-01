package software.wings.graphql.datafetcher.artifactSource.batch;

import com.google.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.artifactSource.ArtifactSourceController;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

public class ArtifactSourceBatchDataLoader implements MappedBatchLoader<String, QLArtifactSource> {
  final ArtifactStreamService artifactStreamService;

  @Inject
  public ArtifactSourceBatchDataLoader(ArtifactStreamService artifactStreamService) {
    this.artifactStreamService = artifactStreamService;
  }

  @Override
  public CompletionStage<Map<String, QLArtifactSource>> load(Set<String> artifactSourceIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, QLArtifactSource> artifactSourceMap = null;
      if (!CollectionUtils.isEmpty(artifactSourceIds)) {
        artifactSourceMap = getArtifactSourceMap(artifactSourceIds);
      } else {
        artifactSourceMap = Collections.EMPTY_MAP;
      }
      return artifactSourceMap;
    });
  }

  public Map<String, QLArtifactSource> getArtifactSourceMap(@NotNull Set<String> artifactSourceIds) {
    List<ArtifactStream> artifactStreamList = artifactStreamService.listByIds(artifactSourceIds);

    return artifactStreamList.stream()
        .map(ArtifactSourceController::populateArtifactSource)
        .collect(Collectors.toMap(QLArtifactSource::getId, Function.identity()));
  }
}
