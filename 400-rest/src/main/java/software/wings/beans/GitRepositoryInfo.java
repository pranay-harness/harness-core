/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * This repository info contains fields for UI purpose to show complete url of git repository, displayUrl which is short
 * url of repository and repository provider
 */
@Data
@Builder
public class GitRepositoryInfo {
  private String url;
  private String displayUrl;
  private GitProvider provider;

  public enum GitProvider { GITHUB, BITBUCKET, GITLAB, UNKNOWN }
}
