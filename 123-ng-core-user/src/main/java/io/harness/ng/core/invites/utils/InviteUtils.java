/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.invites.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class InviteUtils {
  private static final String NG_UI_PATH_PREFIX = "ng/";

  public URI getResourceUrl(String baseUrl, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    URIBuilder uriBuilder = null;
    try {
      uriBuilder = new URIBuilder(baseUrl);
    } catch (URISyntaxException e) {
      log.error("Error building URIBuilder from baseUrl: " + baseUrl, e);
      throw new WingsException(e);
    }

    uriBuilder.setPath(NG_UI_PATH_PREFIX);

    String resourceUrl = String.format("/account/%s/home/get-started", accountIdentifier);
    if (isNotEmpty(projectIdentifier)) {
      resourceUrl = String.format(
          "/account/%s/home/orgs/%s/projects/%s/details", accountIdentifier, orgIdentifier, projectIdentifier);
    } else if (isNotEmpty(orgIdentifier)) {
      resourceUrl = String.format("/account/%s/settings/organizations/%s/details", accountIdentifier, orgIdentifier);
    }

    uriBuilder.setFragment(resourceUrl);
    try {
      return uriBuilder.build();
    } catch (URISyntaxException e) {
      log.error("Error building resourceUrl", e);
      throw new WingsException(e);
    }
  }
}
