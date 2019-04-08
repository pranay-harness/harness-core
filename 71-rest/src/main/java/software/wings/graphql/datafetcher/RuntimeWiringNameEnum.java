package software.wings.graphql.datafetcher;

import lombok.Getter;

/**
 * In this enum e can define the root or parent graphql object type.
 *
 * So, when we do nested queries we can refer to these root or parent types.
 */
public enum RuntimeWiringNameEnum {
  QUERY("Query"),
  WORKFLOW("Workflow");

  @Getter private String typeName;

  RuntimeWiringNameEnum(String typeName) {
    this.typeName = typeName;
  }
}
