package software.wings.service.impl.servicenow;

import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import okhttp3.Credentials;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.helpers.ext.servicenow.ServiceNowRestClient;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowState;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ServiceNowDelegateServiceImpl implements ServiceNowDelegateService {
  private static final Logger logger = LoggerFactory.getLogger(ServiceNowDelegateServiceImpl.class);
  private static final int TIMEOUT = 10 * 1000;
  @Inject private EncryptionService encryptionService;

  @Override
  public boolean validateConnector(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    final Call<JsonNode> request;
    request = getRestClient(taskParameters)
                  .validateConnection(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      handleResponse(response, "Failed to validate ServiceNow credentials");
      return true;
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Failed to authenticate to servicenow");
      if (e instanceof SocketTimeoutException) {
        throw new WingsException(ErrorCode.INVALID_TICKETING_SERVER, USER, e)
            .addParam("message",
                e.getMessage() + "."
                    + "SocketTimeout: ServiceNow server may not be running");
      }
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<ServiceNowState> getStates(ServiceNowTaskParameters taskParameters) {
    final Call<JsonNode> request;
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    switch (taskParameters.getTicketType()) {
      case INCIDENT:
        request = getRestClient(taskParameters)
                      .getIncidentStates(Credentials.basic(config.getUsername(), new String(config.getPassword())));
        break;
      case PROBLEM:
        request = getRestClient(taskParameters)
                      .getProblemStates(Credentials.basic(config.getUsername(), new String(config.getPassword())));
        break;
      case CHANGE_REQUEST:
        request =
            getRestClient(taskParameters)
                .getChangeRequestStates(Credentials.basic(config.getUsername(), new String(config.getPassword())));
        break;
      default:
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam("message", "Invalid ticket type : " + taskParameters.getTicketType());
    }

    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to fetch States from serviceNow");
      List<ServiceNowState> responseStates = new ArrayList<>();

      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        for (JsonNode stateObj : responseObj) {
          ServiceNowState serviceNowState = ServiceNowState.builder()
                                                .id(stateObj.get("value").intValue())
                                                .displayName(stateObj.get("label").textValue())
                                                .build();
          responseStates.add(serviceNowState);
        }
      }
      logger.info("States for ticketType {}: {}", taskParameters.getTicketType(), responseStates);
      return responseStates;
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String errorMsg = "Error in fetching states from serviceNow";
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public String getIssueUrl(ServiceNowTaskParameters taskParameters) {
    final Call<JsonNode> request;
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    String query = "number=" + taskParameters.getIssueNumber();
    request = getRestClient(taskParameters)
                  .getIssueId(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                      taskParameters.getTicketType().toString().toLowerCase(), query);

    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to fetch IssueId : " + taskParameters.getIssueNumber() + " from serviceNow");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        JsonNode issueObj = responseObj.get(0);
        if (issueObj != null) {
          String issueId = issueObj.get("sys_id").asText();
          return getBaseUrl(config) + "nav_to.do?uri=/" + taskParameters.getTicketType().toString().toLowerCase()
              + ".do?sys_id=" + issueId;
        } else {
          String errorMsg = "Error in fetching issue " + taskParameters.getIssueNumber() + " .Issue does not exist";
          throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", errorMsg);
        }

      } else {
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam(
                "message", "Failed to fetch issueNumber " + taskParameters.getIssueNumber() + "response: " + response);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String errorMsg = "Error in fetching issueNumber " + taskParameters.getIssueNumber();
      throw new WingsException(SERVICENOW_ERROR, USER, e)
          .addParam("message", errorMsg + " " + ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public String getIssueStatus(ServiceNowTaskParameters taskParameters) {
    final Call<JsonNode> request;
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    String query = "number=" + taskParameters.getIssueNumber();
    request = getRestClient(taskParameters)
                  .getIssueStatus(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                      taskParameters.getTicketType().toString().toLowerCase(), query, "all");
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to fetch IssueId : " + taskParameters.getIssueNumber() + " from serviceNow");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        JsonNode issueObj = responseObj.get(0);
        if (issueObj != null) {
          return issueObj.get("state").get("display_value").asText();
        } else {
          String errorMsg = "Error in fetching issue " + taskParameters.getIssueNumber() + " .Issue does not exist";
          throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", errorMsg);
        }

      } else {
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam(
                "message", "Failed to fetch issueNumber " + taskParameters.getIssueNumber() + "response: " + response);
      }
    } catch (Exception e) {
      String errorMsg = "Error while fetching issueNumber" + taskParameters.getIssueNumber();
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  private String getBaseUrl(ServiceNowConfig snowConfig) {
    String baseUrl = snowConfig.getBaseUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }

    return baseUrl;
  }

  private CloseableHttpClient getSnowClient(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    encryptionService.decrypt(config, taskParameters.getEncryptionDetails());

    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials =
        new UsernamePasswordCredentials(config.getUsername(), new String(config.getPassword()));
    credsProvider.setCredentials(AuthScope.ANY, credentials);

    RequestConfig requestConfig = RequestConfig.custom()
                                      .setConnectTimeout(TIMEOUT)
                                      .setConnectionRequestTimeout(TIMEOUT)
                                      .setSocketTimeout(TIMEOUT)
                                      .build();

    return HttpClientBuilder.create()
        .setDefaultRequestConfig(requestConfig)
        .setDefaultCredentialsProvider(credsProvider)
        .build();
  }

  private void handleResponse(Response<?> response, String message) throws IOException {
    if (response.isSuccessful()) {
      return;
    }
    if (response.code() == 401) {
      throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", "Invalid ServiceNow credentials");
    }
    if (response.code() == 404) {
      throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", "404 Not found");
    }
    if (response.errorBody() == null) {
      throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", message + " : " + response.message());
    }
    throw new WingsException(SERVICENOW_ERROR, USER)
        .addParam("message", message + " : " + response.errorBody().string());
  }

  private ServiceNowRestClient getRestClient(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    encryptionService.decrypt(config, taskParameters.getEncryptionDetails());
    return getRetrofit(getBaseUrl(config), JacksonConverterFactory.create()).create(ServiceNowRestClient.class);
  }

  private static Retrofit getRetrofit(String baseUrl, Converter.Factory converterFactory) {
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(Http.getUnsafeOkHttpClient(baseUrl))
        .build();
  }
}
