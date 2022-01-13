/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifactSource.batch;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.graphql.datafetcher.artifactSource.ArtifactSourceController.populateArtifactSource;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;

@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
    Map<String, QLArtifactSource> result = new HashMap<>();
    if (isNotEmpty(artifactStreamList)) {
      for (ArtifactStream artifactStream : artifactStreamList) {
        if (!artifactStream.isArtifactStreamParameterized()) {
          result.put(artifactStream.getUuid(), populateArtifactSource(artifactStream));
        } else {
          List<String> parameters = artifactStreamService.getArtifactStreamParameters(artifactStream.getUuid());
          result.put(artifactStream.getUuid(), populateArtifactSource(artifactStream, parameters));
        }
      }
    }
    return result;
  }
}
