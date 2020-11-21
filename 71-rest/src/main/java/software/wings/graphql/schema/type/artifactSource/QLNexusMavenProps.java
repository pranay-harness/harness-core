package software.wings.graphql.schema.type.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SERVICE)
public class QLNexusMavenProps implements QLNexusProps {
  String nexusConnectorId;
  String repository;
  QLNexusRepositoryFormat repositoryFormat;
  String groupId;
  List<String> artifactId;
  String extension;
  String classifier;
}
