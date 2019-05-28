package io.harness.testframework.restutils;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.mapper.ObjectMapperType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;

import java.io.File;
import javax.ws.rs.core.GenericType;

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
}
