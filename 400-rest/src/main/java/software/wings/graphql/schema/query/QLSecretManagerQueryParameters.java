package software.wings.graphql.schema.query;

import lombok.Value;

@Value
public class QLSecretManagerQueryParameters {
  private String secretManagerId;
  private String name;
}
