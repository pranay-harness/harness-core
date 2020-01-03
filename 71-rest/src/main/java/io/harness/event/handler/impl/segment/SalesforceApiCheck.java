package io.harness.event.handler.impl.segment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.handler.segment.SalesforceConfig;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import software.wings.beans.Account;

import java.io.IOException;
import javax.security.auth.login.LoginException;

@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesforceApiCheck {
  private static final String REST_ENDPOINT = "/services/data";
  private static final String OAUTH_ENDPOINT = "/services/oauth2/token";
  private static final String INSTANCE_URL = "instance_url";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String AUTHORIZATION = "Authorization";
  private static final int CONNECTION_TIMEOUT = 10000;
  private static final int CONNECTION_REQUEST_TIMEOUT = 10000;
  private static final int MAX_RETRIES = 3;

  Header prettyPrintHeader = new BasicHeader("X-PrettyPrint", "1");
  HttpClient httpClient;
  RequestConfig requestConfig;
  SalesforceConfig salesforceConfig;
  String loginUrl;

  @Inject
  public SalesforceApiCheck(SalesforceConfig salesforceConfig) {
    this.salesforceConfig = salesforceConfig;
    this.loginUrl = getLoginUrl();
    this.requestConfig = RequestConfig.custom()
                             .setConnectTimeout(CONNECTION_TIMEOUT)
                             .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                             .build();
    this.httpClient = HttpClientBuilder.create().build();
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

  private String login() throws LoginException {
    HttpPost httpPost = new HttpPost(loginUrl);
    httpPost.setConfig(requestConfig);
    HttpResponse response = null;
    try {
      response = httpClient.execute(httpPost);
    } catch (ClientProtocolException cpe) {
      logger.error("Error when connecting to Salesforce with userName={}", salesforceConfig.getUserName());
    } catch (IOException ioe) {
      logger.error("Error in connecting to Salesforce with userName={}", salesforceConfig.getUserName());
    }

    if (response == null) {
      logger.error("Login Response is null with given credentials in Salesforce");
      return null;
    }
    if (response.getStatusLine() != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      logger.info("Successfully logged in to Salesforce, with userName={}", salesforceConfig.getUserName());
    }
    String responseString = null;
    try {
      responseString = EntityUtils.toString(response.getEntity());
    } catch (IOException ioe) {
      logger.error("Could not convert Http response to EntityUtils String, where response is {}", response);
    }
    return responseString;
  }

  private String getLoginResponse() {
    String loginResponse = null;
    try {
      loginResponse = login();
    } catch (LoginException le) {
      logger.error("Could not login to Salesforce with given userName={}", salesforceConfig.getUserName());
    }
    return loginResponse;
  }

  private HttpResponse querySalesforce(String queryString) {
    HttpResponse response = null;
    String uri = null;
    int count = 0;
    String loginResponseString = null;

    while (true) {
      loginResponseString = getLoginResponse();
      count++;
      if (loginResponseString != null) {
        break;
      }
      if (count >= MAX_RETRIES) {
        return null;
      }
    }

    try {
      JSONObject json = new JSONObject(loginResponseString);
      String baseUri = json.getString(INSTANCE_URL) + REST_ENDPOINT + "/v" + salesforceConfig.getApiVersion();

      uri = new URIBuilder().setPath(baseUri).setCustomQuery(queryString).toString();
      HttpGet httpGet = new HttpGet(uri);
      httpGet.setConfig(requestConfig);
      Header oauthHeader = new BasicHeader(AUTHORIZATION, "OAuth " + json.getString(ACCESS_TOKEN));

      httpGet.addHeader(oauthHeader);
      httpGet.addHeader(prettyPrintHeader);

      response = httpClient.execute(httpGet);

      int statusCode = response.getStatusLine().getStatusCode();

      if (statusCode == 200) {
        logger.info("Successfully queried Salesforce with query={}", queryString);
        return response;
      }
    } catch (JSONException je) {
      logger.error("Error in Jsonifying Http Response {}", loginResponseString);
    } catch (IOException ioe) {
      logger.error("Could not connect to Salesforce with URI {}", uri);
    }
    return response;
  }

  private boolean isFoundInSalesforce(String queryString) {
    int count = 0;
    HttpResponse httpResponse = null;

    while (true) {
      httpResponse = querySalesforce(queryString);
      count++;
      if (httpResponse != null && httpResponse.getStatusLine() != null
          && httpResponse.getStatusLine().getStatusCode() == 200) {
        break;
      }
      if (count >= MAX_RETRIES) {
        logger.error("Response from Salesforce is null");
        return false;
      }
    }

    try {
      String responseString = EntityUtils.toString(httpResponse.getEntity());
      try {
        JSONObject jsonObject = new JSONObject(responseString);
        if (jsonObject.has("totalSize")) {
          if ((Integer) jsonObject.get("totalSize") > 0) {
            logger.info("The account has been found in Salesforce, with response={}", jsonObject);
            return true;
          } else {
            logger.info("The account was not found in Salesforce when sending group calls to Segment");
            return false;
          }
        }
      } catch (JSONException je) {
        logger.error("Error while jsonifying responseString={}", responseString);
      }
    } catch (IOException ioe) {
      logger.error("Error while converting response={} to String", httpResponse);
    }
    return false;
  }

  public boolean isPresentInSalesforce(Account account) {
    if (account == null || account.getAccountName() == null) {
      return false;
    }
    String queryString = createSOQLQuery(account);
    return isFoundInSalesforce(queryString);
  }

  private String createSOQLQuery(Account account) {
    return "/query?q="
        + String.format("select+id+,+name+from+account+where+Harness_Account_ID__c='%1$s'", account.getUuid());
  }
}
