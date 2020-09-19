package software.wings.graphql.datafetcher.connector.utils;

import io.harness.utils.RequestField;
import software.wings.graphql.schema.mutation.connector.input.QLDockerConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLDockerConnectorInput.QLDockerConnectorInputBuilder;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput;
import software.wings.graphql.schema.mutation.connector.input.QLGitConnectorInput.QLGitConnectorInputBuilder;

public class Utility {
  public static QLGitConnectorInputBuilder getQlGitConnectorInputBuilder() {
    return QLGitConnectorInput.builder()
        .name(RequestField.ofNullable("NAME"))
        .URL(RequestField.ofNullable("URL"))
        .userName(RequestField.ofNullable("USER"))
        .branch(RequestField.absent())
        .passwordSecretId(RequestField.absent())
        .sshSettingId(RequestField.absent())
        .generateWebhookUrl(RequestField.absent())
        .customCommitDetails(RequestField.absent());
  }

  public static QLDockerConnectorInputBuilder getQlDockerConnectorInputBuilder() {
    return QLDockerConnectorInput.builder()
        .name(RequestField.ofNullable("NAME"))
        .URL(RequestField.ofNullable("URL"))
        .userName(RequestField.ofNullable("USER"))
        .passwordSecretId(RequestField.absent());
  }
}
