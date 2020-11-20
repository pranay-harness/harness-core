package io.harness.testframework.restutils;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import lombok.experimental.UtilityClass;
import software.wings.search.framework.SearchResults;

import javax.ws.rs.core.GenericType;

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
