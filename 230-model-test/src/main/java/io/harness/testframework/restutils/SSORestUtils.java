/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;

import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.helpers.ext.ldap.LdapResponse;

import com.google.gson.JsonObject;
import io.restassured.mapper.ObjectMapperType;
import java.io.File;
import java.util.Collection;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SSORestUtils {
  public static Integer addOauthSettings(String accountId, String bearerToken, OauthSettings oauthSettings) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .body(oauthSettings, ObjectMapperType.GSON)
        .post("/sso/oauth-settings-upload")
        .getStatusCode();
  }

  public static Object getAccessManagementSettings(String accountId, String bearerToken) {
    RestResponse<Object> oauthSettings =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .get("/sso/access-management/" + accountId)
            .as(new GenericType<RestResponse<Object>>() {}.getType(), ObjectMapperType.GSON);
    return oauthSettings.getResource();
  }

  public static Integer addLdapSettings(String accountId, String bearerToken, LdapSettings ldapSettings) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .body(ldapSettings, ObjectMapperType.GSON)
        .post("/sso/ldap/settings")
        .getStatusCode();
  }

  public static Integer getLdapSettings(String accountId, String bearerToken) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .get("/sso/ldap/settings")
        .getStatusCode();
  }

  public static Integer addSAMLSettings(String accountId, String bearerToken, String name, String fileName) {
    File file = new File(fileName);
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .contentType(MULTIPART_FORM_DATA)
        .queryParam("accountId", accountId)
        .formParam("displayName", name)
        .multiPart("file", file, "text/html")
        .formParam("authorizationEnabled", "false")
        .post("/sso/saml-idp-metadata-upload")
        .getStatusCode();
  }

  public static Integer deleSAMLSettings(String accountId, String bearerToken) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .delete("/sso/delete-saml-idp-metadata")
        .getStatusCode();
  }

  public static Integer deleteLDAPSettings(String accountId, String bearerToken) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .delete("/sso/ldap/settings")
        .getStatusCode();
  }

  public static Integer deleteOAUTHSettings(String accountId, String bearerToken) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .delete("/sso/delete-oauth-settings")
        .getStatusCode();
  }

  public static Collection<LdapGroupResponse> searchLdapWithQuery(
      String accountId, String bearerToken, String query, String ldapId) {
    RestResponse<Collection<LdapGroupResponse>> searchResults =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", accountId)
            .queryParam("q", query)
            .get("/sso/ldap/" + ldapId + "/search/group")
            .as(new GenericType<RestResponse<Collection<LdapGroupResponse>>>() {}.getType());
    return searchResults.getResource();
  }

  public static LdapResponse testAuthenticate(String accountId, String bearerToken, String emailId, String password) {
    JsonObject jObj = new JsonObject();
    jObj.addProperty("email", emailId);
    jObj.addProperty("password", password);
    RestResponse<LdapResponse> searchResults = Setup.portal()
                                                   .auth()
                                                   .oauth2(bearerToken)
                                                   .queryParam("accountId", accountId)
                                                   .body(jObj.toString())
                                                   .post("/sso/ldap/settings/test/authentication")
                                                   .as(new GenericType<RestResponse<LdapResponse>>() {}.getType());
    return searchResults.getResource();
  }

  public static Integer assignAuthMechanism(String accountId, String bearerToken, String authMechanism) {
    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .queryParam("authMechanism", authMechanism)
        .put("/sso/assign-auth-mechanism")
        .getStatusCode();
  }
}
