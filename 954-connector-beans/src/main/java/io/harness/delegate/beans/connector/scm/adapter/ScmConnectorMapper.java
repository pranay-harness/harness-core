/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.scm.adapter;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class ScmConnectorMapper {
  public static GitConfigDTO toGitConfigDTO(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnectorDTO) {
      return GithubToGitMapper.mapToGitConfigDTO((GithubConnectorDTO) scmConnector);
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      return GitlabToGitMapper.mapToGitConfigDTO((GitlabConnectorDTO) scmConnector);
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      return BitbucketToGitMapper.mapToGitConfigDTO((BitbucketConnectorDTO) scmConnector);
    } else {
      return (GitConfigDTO) scmConnector;
    }
  }
}
