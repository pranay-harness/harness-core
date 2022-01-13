/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
