/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.EntityType;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.stencils.DataProvider;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;

@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
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
