/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cistatus.service.gitlab;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString(exclude = "personalAccessToken")
public class GitlabConfig {
  String personalAccessToken;
  @NotNull String gitlabUrl;
}
