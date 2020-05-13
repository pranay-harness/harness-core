package software.wings.graphql.datafetcher.artifactSource.batch;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;
import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.concurrent.CompletionStage;

@OwnedBy(CDC)
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
