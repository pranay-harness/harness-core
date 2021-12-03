package io.harness.delegate.beans.connector.scm.gitlab;

import static io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorConstants.TOKEN;

import io.harness.beans.DecryptableEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = GitlabTokenSpecDTO.class, name = TOKEN) })
@Schema(name = "GitlabApiAccessSpec",
    description =
        "This contains details of the information such as references of username and password needed for Gitlab API access")
public interface GitlabApiAccessSpecDTO extends DecryptableEntity {}
