/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.graphql.datafetcher.artifactSource.ArtifactSourceController.populateArtifactSource;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactSourceDataFetcher extends AbstractObjectDataFetcher<QLArtifactSource, QLArtifactSourceQueryParam> {
  @Inject ArtifactStreamService artifactStreamService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLArtifactSource fetch(QLArtifactSourceQueryParam parameters, String accountId) {
    String artifactSourceId = parameters.getArtifactSourceId();

    if (artifactSourceId != null) {
      ArtifactStream artifactStream = artifactStreamService.get(artifactSourceId);
      if (!artifactStream.isArtifactStreamParameterized()) {
        return populateArtifactSource(artifactStream);
      } else {
        List<String> params = artifactStreamService.getArtifactStreamParameters(artifactStream.getUuid());
        return populateArtifactSource(artifactStream, params);
      }
    }

    return null;
  }
}
