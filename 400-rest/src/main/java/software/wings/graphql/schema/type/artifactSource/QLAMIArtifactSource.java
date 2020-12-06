package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLKeyValuePair;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAMIArtifactSourceKeys")
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class QLAMIArtifactSource implements QLArtifactSource {
  String name;
  String id;
  Long createdAt;
  String region;
  String awsCloudProviderId;
  List<QLKeyValuePair> awsTags;
  List<QLKeyValuePair> amiResourceFilters;
}
