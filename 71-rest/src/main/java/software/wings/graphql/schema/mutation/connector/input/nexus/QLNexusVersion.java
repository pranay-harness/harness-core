package software.wings.graphql.schema.mutation.connector.input.nexus;

public enum QLNexusVersion {
  V2("2.x"),
  V3("3.x");

  public final String value;

  QLNexusVersion(String value) {
    this.value = value;
  }
}
