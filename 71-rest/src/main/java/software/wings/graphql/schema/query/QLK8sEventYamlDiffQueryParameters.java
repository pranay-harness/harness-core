package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLK8sEventYamlDiffQueryParameters {
  private String oldYamlRef;
  private String newYamlRef;
}
