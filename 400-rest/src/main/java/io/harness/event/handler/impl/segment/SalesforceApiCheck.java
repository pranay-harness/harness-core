/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.handler.impl.segment;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.handler.segment.SalesforceConfig;

import software.wings.beans.Account;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URISyntaxException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@OwnedBy(PL)
@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesforceApiCheck {
  private static final String REST_ENDPOINT = "/services/data";
  private static final String OAUTH_ENDPOINT = "/services/oauth2/token";
  private static final String INSTANCE_URL = "instance_url";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String AUTHORIZATION = "Authorization";
  private static final int MAX_RETRIES = 3;

  Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
  HttpClient httpClient;
  SalesforceConfig salesforceConfig;
  String loginUrl;
  String salesforceAccountName;

  @Inject
  public SalesforceApiCheck(SalesforceConfig salesforceConfig) {
    this.salesforceConfig = salesforceConfig;
    this.loginUrl = getLoginUrl();
    this.httpClient = HttpClientBuilder.create().build();
  }

  public String getSalesforceAccountName() {
    return salesforceAccountName;
  }

  public boolean isSalesForceIntegrationEnabled() {
    return this.salesforceConfig.isEnabled();
  }

  private String getLoginUrl() {
    String requestBodyText = "?grant_type=password";
    requestBodyText += "&client_id=";
    requestBodyText += salesforceConfig.getConsumerKey();
    requestBodyText += "&client_secret=";
    requestBodyText += salesforceConfig.getConsumerSecret();
    requestBodyText += "&username=";
    requestBodyText += salesforceConfig.getUserName();
    requestBodyText += "&password=";
    requestBodyText += salesforceConfig.getPassword();

    return "https://" + salesforceConfig.getLoginInstanceDomain() + OAUTH_ENDPOINT + requestBodyText;
  }

  private String login() {
    HttpPost httpPost = new HttpPost(loginUrl);
    HttpResponse response = null;
    try {
      try {
        response = httpClient.execute(httpPost);
      } catch (ClientProtocolException cpe) {
        log.error("Error when connecting to Salesforce with userName={}", salesforceConfig.getUserName(), cpe);
      } catch (IOException ioe) {
        log.error("Error in connecting to Salesforce with userName={}", salesforceConfig.getUserName(), ioe);
      }

      if (response == null) {
        log.error("Login Response is null with given credentials in Salesforce");
        return null;
      }
      if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        log.info("Successfully logged in to Salesforce, with userName={}", salesforceConfig.getUserName());
      }
      String responseString = null;
      try {
        responseString = EntityUtils.toString(response.getEntity());
      } catch (IOException ioe) {
        log.error(
            "Could not convert Salesforce Http response to EntityUtils String, where response is {}", response, ioe);
      }
      return responseString;
    } finally {
      httpPost.releaseConnection();
    }
  }

  private String querySalesforce(String queryString) {
    HttpResponse response = null;
    int count = 0;
    String loginResponseString;

    while (true) {
      loginResponseString = login();
      count++;
      if (isNotEmpty(loginResponseString)) {
        break;
      }
      if (count >= MAX_RETRIES) {
        log.error("Response from Salesforce Login is null");
        return null;
      }
    }

    JSONObject json = new JSONObject(loginResponseString);
    String baseUri = json.getString(INSTANCE_URL) + REST_ENDPOINT + "/v47.0/query";

    String uri = null;
    try {
      uri = new URIBuilder(baseUri).setCustomQuery(queryString).toString();
    } catch (URISyntaxException e) {
      log.error("Salesforce error in uri building ", e);
    }

    HttpGet httpGet = new HttpGet(uri);
    Header oauthHeader = new BasicHeader(AUTHORIZATION, "OAuth " + json.getString(ACCESS_TOKEN));
    httpGet.addHeader(oauthHeader);
    httpGet.addHeader(prettyPrintHeader);

    try {
      try {
        response = httpClient.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
          log.info("Successfully queried Salesforce with query={}", queryString);
        }
      } catch (JSONException je) {
        log.error("Error in Jsonifying Salesforce Http Response {}", loginResponseString, je);
      } catch (IOException ioe) {
        log.error("Could not connect to Salesforce with URI {}", uri, ioe);
      }
      String responseString = null;
      try {
        if (response != null) {
          responseString = EntityUtils.toString(response.getEntity());
        }
      } catch (IOException ioe) {
        log.error("Error while converting Salesforce response={} to String", response, ioe);
      }
      return responseString;
    } finally {
      httpGet.releaseConnection();
    }
  }

  private String retryQueryResponse(String queryString) {
    int count = 0;
    String responseString;

    while (true) {
      responseString = querySalesforce(queryString);
      count++;
      if (isNotEmpty(responseString)) {
        return responseString;
      }
      if (count >= MAX_RETRIES) {
        log.error("Response from Salesforce Query is null");
        return null;
      }
    }
  }

  private boolean isFoundInSalesforce(String accountId, String queryString) {
    String responseString = retryQueryResponse(queryString);

    if (isEmpty(responseString)) {
      return false;
    }

    JSONObject jsonObject = new JSONObject(responseString);
    try {
      if (jsonObject.has("totalSize")) {
        if ((Integer) jsonObject.get("totalSize") > 0) {
          log.info("The account {} was found in Salesforce, with response={}", accountId, jsonObject);
          try {
            this.salesforceAccountName =
                ((JSONObject) ((JSONArray) jsonObject.get("records")).get(0)).get("Name").toString();
          } catch (JSONException je) {
            log.error("JSON Exception while getting Salesforce Account Name from API", je);
            return false;
          }
          log.info("The account {} was found in Salesforce when sending group calls to Segment", accountId);
          return true;
        } else {
          log.info("The account {} was not found in Salesforce when sending group calls to Segment", accountId);
          return false;
        }
      } else {
        log.error("Salesforce Json object doesn't have field 'totalSize', response={}", jsonObject);
      }
    } catch (JSONException je) {
      log.error("Error while jsonifying Salesforce responseString={}", responseString, je);
    }
    return false;
  }

  public boolean isPresentInSalesforce(Account account) {
    if (account == null || account.getAccountName() == null) {
      return false;
    }
    String queryString = createSOQLQuery(account);
    return isFoundInSalesforce(account.getUuid(), queryString);
  }

  private String createSOQLQuery(Account account) {
    return "q=" + String.format("select+id+,+name+from+account+where+Harness_Account_ID__c='%1$s'", account.getUuid());
  }
}
