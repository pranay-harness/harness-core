package software.wings.service.impl.servicenow;

import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.exception.ExceptionUtils;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.helpers.ext.servicenow.ServiceNowRestClient;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class ServiceNowDelegateServiceImpl implements ServiceNowDelegateService {
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
  public List<ServiceNowMetaDTO> getStates(ServiceNowTaskParameters taskParameters) {
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
      List<ServiceNowMetaDTO> responseStates = new ArrayList<>();

      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        for (JsonNode stateObj : responseObj) {
          ServiceNowMetaDTO serviceNowMetaDTO = ServiceNowMetaDTO.builder()
                                                    .id(stateObj.get("value").textValue())
                                                    .displayName(stateObj.get("label").textValue())
                                                    .build();
          responseStates.add(serviceNowMetaDTO);
        }
      } else {
        // todo: Check this error message with srinivas
        throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", "");
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
  public Map<String, List<ServiceNowMetaDTO>> getCreateMeta(ServiceNowTaskParameters taskParameters) {
    Response<JsonNode> response = null;
    Map<String, List<ServiceNowMetaDTO>> responseMap = new HashMap<>();
    switch (taskParameters.getTicketType()) {
      case INCIDENT:
        responseMap.put("impact", getImpacts(taskParameters));
        responseMap.put("urgency", getUrgency(taskParameters));
        return responseMap;
      case PROBLEM:
        return new HashMap<>();
      case CHANGE_REQUEST:
        responseMap.put("impact", getImpacts(taskParameters));
        responseMap.put("priority", getPriority(taskParameters));
        responseMap.put("risk", getRisk(taskParameters));
        responseMap.put("changeRequestType", getChangeRequestType(taskParameters));
        return responseMap;
      default:
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam("message", "Invalid ticket type : " + taskParameters.getTicketType());
    }
  }

  private List<ServiceNowMetaDTO> getImpacts(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Response<JsonNode> response;
    Call<JsonNode> request = getRestClient(taskParameters)
                                 .getImpact(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "impact");
  }

  private List<ServiceNowMetaDTO> getChangeRequestType(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Response<JsonNode> response;
    Call<JsonNode> request =
        getRestClient(taskParameters)
            .getChangeRequestTypes(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "changeRequestType");
  }

  private List<ServiceNowMetaDTO> getPriority(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Response<JsonNode> response;
    Call<JsonNode> request =
        getRestClient(taskParameters)
            .getPriority(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "priority");
  }

  private List<ServiceNowMetaDTO> getRisk(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Response<JsonNode> response;
    Call<JsonNode> request = getRestClient(taskParameters)
                                 .getRisk(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "risk");
  }

  private List<ServiceNowMetaDTO> getUrgency(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Response<JsonNode> response;
    Call<JsonNode> request = getRestClient(taskParameters)
                                 .getUrgency(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "urgency");
  }

  private List<ServiceNowMetaDTO> handleGetCallForFields(Call<JsonNode> request, String field) {
    Response<JsonNode> response;
    try {
      response = request.execute();
      handleResponse(response, "Failed to fetch " + field + " from serviceNow");
      JsonNode responseObj = response.body().get("result");
      List<ServiceNowMetaDTO> fields = new ArrayList<>();
      if (responseObj.isArray()) {
        for (JsonNode impactObj : responseObj) {
          ServiceNowMetaDTO serviceNowMetaDTO = ServiceNowMetaDTO.builder()
                                                    .id(impactObj.get("value").textValue())
                                                    .displayName(impactObj.get("label").textValue())
                                                    .build();
          fields.add(serviceNowMetaDTO);
        }
        return fields;
      } else {
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam("message", "Response for fetching " + field + " is not an array. Response: " + response);
      }
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      String errorMsg = "Error in fetching " + field + " from serviceNow";
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

  public static void handleResponse(Response<?> response, String message) throws IOException {
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

  public ServiceNowRestClient getRestClient(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    encryptionService.decrypt(config, taskParameters.getEncryptionDetails());
    return getRetrofit(getBaseUrl(config), JacksonConverterFactory.create()).create(ServiceNowRestClient.class);
  }

  public static Retrofit getRetrofit(String baseUrl, Converter.Factory converterFactory) {
    return new Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(converterFactory)
        .client(Http.getUnsafeOkHttpClient(baseUrl))
        .build();
  }

  public static String getBaseUrl(ServiceNowConfig snowConfig) {
    String baseUrl = snowConfig.getBaseUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl;
  }
}
