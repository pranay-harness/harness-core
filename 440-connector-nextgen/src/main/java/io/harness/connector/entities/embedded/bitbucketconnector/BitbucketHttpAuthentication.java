/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.entities.embedded.bitbucketconnector;

import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.connector.entities.embedded.bitbucketconnector.BitbucketHttpAuthentication")
public class BitbucketHttpAuthentication implements BitbucketAuthentication {
  BitbucketHttpAuthenticationType type;
  BitbucketHttpAuth auth;
}
