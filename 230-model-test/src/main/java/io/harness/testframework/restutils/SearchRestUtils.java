/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.search.framework.SearchResults;

import io.restassured.http.ContentType;
import javax.ws.rs.core.GenericType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SearchRestUtils {
  public static SearchResults search(String bearerToken, String accountId, String query) {
    GenericType<RestResponse<SearchResults>> searchResponseType = new GenericType<RestResponse<SearchResults>>() {};
    RestResponse<SearchResults> searchResponseRestResponse = Setup.portal()
                                                                 .auth()
                                                                 .oauth2(bearerToken)
                                                                 .queryParam("accountId", accountId)
                                                                 .queryParam("query", query)
                                                                 .contentType(ContentType.JSON)
                                                                 .get("/search")
                                                                 .as(searchResponseType.getType());
    assertThat(searchResponseRestResponse.getResource()).isNotNull();

    return searchResponseRestResponse.getResource();
  }
}
