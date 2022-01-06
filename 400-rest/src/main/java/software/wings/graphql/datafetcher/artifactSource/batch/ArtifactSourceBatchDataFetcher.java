/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.artifactSource.batch;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactStreamService;

import com.google.inject.Inject;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;

@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactSourceBatchDataFetcher
    extends AbstractBatchDataFetcher<QLArtifactSource, QLArtifactSourceQueryParam, String> {
  final ArtifactStreamService artifactStreamService;
  @Inject
  public ArtifactSourceBatchDataFetcher(ArtifactStreamService artifactStreamService) {
    this.artifactStreamService = artifactStreamService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected CompletionStage<QLArtifactSource> load(
      QLArtifactSourceQueryParam parameters, DataLoader<String, QLArtifactSource> dataLoader) {
    final String artifactSourceId;
    if (StringUtils.isNotBlank(parameters.getArtifactSourceId())) {
      artifactSourceId = parameters.getArtifactSourceId();
    } else {
      throw new InvalidRequestException("Artifact Source Id not present in query", WingsException.USER);
    }
    return dataLoader.load(artifactSourceId);
  }
}
