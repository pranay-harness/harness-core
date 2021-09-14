/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.entities.embedded.gitlabconnector;

import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.gitlabconnector.GitlabHttpAuthentication")
public class GitlabHttpAuthentication implements GitlabAuthentication {
  GitlabHttpAuthenticationType type;
  GitlabHttpAuth auth;
}
