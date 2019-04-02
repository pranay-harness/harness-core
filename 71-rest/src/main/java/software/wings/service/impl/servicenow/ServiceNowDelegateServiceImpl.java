package software.wings.service.impl.servicenow;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ServiceNowConfig;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Singleton
public class ServiceNowDelegateServiceImpl implements ServiceNowDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(ServiceNowDelegateServiceImpl.class);
  private static final int TIMEOUT = 10 * 1000;

  @Override
  public void validateConnector(ServiceNowConfig snowConfig) {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials(snowConfig.getUsername(), new String(snowConfig.getPassword()));
    credsProvider.setCredentials(AuthScope.ANY, credentials);

    RequestConfig config = RequestConfig.custom()
                               .setConnectTimeout(TIMEOUT)
                               .setConnectionRequestTimeout(TIMEOUT)
                               .setSocketTimeout(TIMEOUT)
                               .build();

    String baseUrl = snowConfig.getBaseUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    HttpGet httpget = new HttpGet(baseUrl + "api/now/table/incident?sysparm_limit=1");
    httpget.setHeader("Accept", "application/json");

    try (CloseableHttpClient httpclient = HttpClientBuilder.create()
                                              .setDefaultRequestConfig(config)
                                              .setDefaultCredentialsProvider(credsProvider)
                                              .build();
         CloseableHttpResponse response = httpclient.execute(httpget)) {
      if (response.getStatusLine().getStatusCode() == 200) {
        logger.info("Authenticated to servicenow xxxxxxxx " + response);
        return;
      }
      throwException(response);
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      logger.error("Failed to authenticate to servicenow", e);
      if (e instanceof SocketTimeoutException) {
        throw new WingsException(ErrorCode.INVALID_TICKETING_SERVER, USER)
            .addParam("message",
                e.getMessage() + "."
                    + "SocketTimeout: ServiceNow server may not be running");
      }
      throw new WingsException(ErrorCode.SERVICENOW_ERROR, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  private void throwException(CloseableHttpResponse response) {
    logger.error("Failed to authenticate to ServiceNow. Response:  " + response);
    String errorMsg;
    try {
      errorMsg = new JSONObject(EntityUtils.toString(response.getEntity())).getJSONObject("error").getString("message");
    } catch (JSONException | IOException e) {
      logger.error("error reading errMessage from http response");
      errorMsg = response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();
    }
    throw new WingsException(ErrorCode.SERVICENOW_ERROR, USER).addParam("message", errorMsg);
  }
}
