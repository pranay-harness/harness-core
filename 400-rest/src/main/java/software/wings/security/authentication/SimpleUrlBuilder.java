/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class SimpleUrlBuilder {
  @SuppressWarnings("PMD.AvoidStringBufferField") private StringBuilder builder;
  private boolean hasQueryParams;

  public SimpleUrlBuilder(String baseUrl) {
    this.builder = new StringBuilder(baseUrl);
  }

  public SimpleUrlBuilder addQueryParam(String key, String value) {
    if (this.hasQueryParams) {
      builder.append('&');
    } else {
      builder.append('?');
    }

    builder.append(key).append('=').append(value);
    this.hasQueryParams = true;
    return this;
  }

  public String build() {
    return this.builder.toString();
  }
}
