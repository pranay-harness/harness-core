package io.harness.delegate.beans.connector.scm.gitlab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("GitlabConnectorCredentials")
@Schema(name = "GithubConnectorCredentials",
    description = "This is a interface for details of the Github connector credentials")
public class GitlabConnectorCredentialsDTO {}
