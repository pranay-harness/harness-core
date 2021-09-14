/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;

import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;

@Singleton
@OwnedBy(DX)
public class ScmGitProviderHelper {
  public String getSlug(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnectorDTO) {
      return getSlugFromUrl(((GithubConnectorDTO) scmConnector).getUrl());
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      return getSlugFromUrl(((GitlabConnectorDTO) scmConnector).getUrl());
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      return getSlugFromUrl(((BitbucketConnectorDTO) scmConnector).getUrl());
    } else {
      throw new NotImplementedException(
          String.format("The scm apis for the provider type %s is not supported", scmConnector.getClass()));
    }
  }

  private String getSlugFromUrl(String url) {
    String repUrl = url;
    if (url.endsWith(".git")) {
      int index = url.lastIndexOf(".git");
      if (index > 0) {
        repUrl = url.substring(0, index);
      }
    }
    String[] urlSplited = repUrl.split("/");
    int numberOfValuesSplitted = urlSplited.length;
    return urlSplited[numberOfValuesSplitted - 2] + "/" + urlSplited[numberOfValuesSplitted - 1];
  }
}
