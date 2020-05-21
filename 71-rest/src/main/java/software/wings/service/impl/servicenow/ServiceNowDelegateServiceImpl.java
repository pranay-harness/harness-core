package software.wings.service.impl.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ServiceNowException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.WordUtils;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.helpers.ext.servicenow.ServiceNowRestClient;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.servicenow.ServiceNowDelegateService;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class ServiceNowDelegateServiceImpl implements ServiceNowDelegateService {
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
      logger.error("Failed to authenticate to servicenow. ");
      if (e instanceof SocketTimeoutException) {
        throw new WingsException(ErrorCode.INVALID_TICKETING_SERVER, USER, e)
            .addParam("message",
                e.getMessage() + "."
                    + "SocketTimeout: ServiceNow server may not be running");
      }
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
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
      case CHANGE_TASK:
        request = getRestClient(taskParameters)
                      .getChangeTaskStates(Credentials.basic(config.getUsername(), new String(config.getPassword())));
        break;
      default:
        throw new ServiceNowException(
            "Invalid ticket type : " + taskParameters.getTicketType(), SERVICENOW_ERROR, USER);
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
      throw new ServiceNowException(errorMsg + ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
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
      case CHANGE_TASK:
        responseMap.put("changeRequestNumber", new ArrayList<>());
        responseMap.put("changeTaskType", getChangeTaskType(taskParameters));
        return responseMap;
      default:
        throw new ServiceNowException(
            "Invalid ticket type : " + taskParameters.getTicketType(), SERVICENOW_ERROR, USER);
    }
  }

  @Override
  public List<ServiceNowMetaDTO> getAdditionalFields(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    List<String> alreadySupportedFieldNames =
        Arrays.stream(ServiceNowFields.values()).map(ServiceNowFields::getJsonBodyName).collect(Collectors.toList());

    Call<JsonNode> request =
        getRestClient(taskParameters)
            .getAdditionalFields(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                taskParameters.getTicketType().toString().toLowerCase());
    Response<JsonNode> response = null;
    List<ServiceNowMetaDTO> fields = new ArrayList<>();
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response,
          "Failed to fetch Additional fields for ticketType : " + taskParameters.getTicketType() + " from serviceNow");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        for (JsonNode fieldObj : responseObj) {
          if (!alreadySupportedFieldNames.contains(fieldObj.get("name").textValue())) {
            ServiceNowMetaDTO field = ServiceNowMetaDTO.builder()
                                          .displayName(WordUtils.capitalizeFully(fieldObj.get("label").textValue()))
                                          .id(fieldObj.get("name").textValue())
                                          .build();
            fields.add(field);
          }
        }
        return fields;
      } else {
        throw new ServiceNowException("Failed to fetch additional fields for ticket type "
                + taskParameters.getTicketType() + " response: " + response,
            SERVICENOW_ERROR, USER);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String errorMsg = "Failed to fetch additional fields for ticket type " + taskParameters.getTicketType();
      throw new ServiceNowException(errorMsg + " " + ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private List<ServiceNowMetaDTO> getImpacts(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Call<JsonNode> request = getRestClient(taskParameters)
                                 .getImpact(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "impact");
  }

  private List<ServiceNowMetaDTO> getChangeRequestType(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Call<JsonNode> request =
        getRestClient(taskParameters)
            .getChangeRequestTypes(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "changeRequestType");
  }

  private List<ServiceNowMetaDTO> getChangeTaskType(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Call<JsonNode> request =
        getRestClient(taskParameters)
            .getChangeTaskTypes(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "changeTaskType");
  }

  private List<ServiceNowMetaDTO> getPriority(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Call<JsonNode> request =
        getRestClient(taskParameters)
            .getPriority(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "priority");
  }

  private List<ServiceNowMetaDTO> getRisk(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    Call<JsonNode> request = getRestClient(taskParameters)
                                 .getRisk(Credentials.basic(config.getUsername(), new String(config.getPassword())));
    return handleGetCallForFields(request, "risk");
  }

  private List<ServiceNowMetaDTO> getUrgency(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
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
        throw new ServiceNowException(
            "Response for fetching " + field + " is not an array. Response: " + response, SERVICENOW_ERROR, USER);
      }
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      String errorMsg = "Error in fetching " + field + " from serviceNow. ";
      throw new ServiceNowException(errorMsg + ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private JsonNode getIssue(ServiceNowTaskParameters taskParameters) {
    final Call<JsonNode> request;
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    String query = "number=" + taskParameters.getIssueNumber();
    request = getRestClient(taskParameters)
                  .getIssue(Credentials.basic(config.getUsername(), new String(config.getPassword())),
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
          return issueObj;
        } else {
          String errorMsg = "Error in fetching issue " + taskParameters.getIssueNumber() + " .Issue does not exist";
          throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", errorMsg);
        }

      } else {
        throw new ServiceNowException(
            "Failed to fetch issueNumber " + taskParameters.getIssueNumber() + "response: " + response,
            SERVICENOW_ERROR, USER);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String errorMsg = "Error in fetching issueNumber " + taskParameters.getIssueNumber();
      throw new ServiceNowException(errorMsg + " " + ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  @Override
  public ServiceNowExecutionData getIssueUrl(ServiceNowTaskParameters taskParameters) {
    JsonNode issueObj = getIssue(taskParameters);
    String issueId = issueObj.get("sys_id").get("display_value").asText();
    String issueUrl = getBaseUrl(taskParameters.getServiceNowConfig()) + "nav_to.do?uri=/"
        + taskParameters.getTicketType().toString().toLowerCase() + ".do?sys_id=" + issueId;
    String state = issueObj.get("state").get("display_value").asText();
    return ServiceNowExecutionData.builder().issueUrl(issueUrl).currentState(state).build();
  }

  @Override
  public String getIssueStatus(ServiceNowTaskParameters taskParameters) {
    JsonNode issueObj = getIssue(taskParameters);
    return issueObj.get("state").get("display_value").asText();
  }

  public static void handleResponse(Response<?> response, String message) throws IOException {
    if (response.isSuccessful()) {
      return;
    }
    if (response.code() == 401) {
      throw new ServiceNowException("Invalid ServiceNow credentials", SERVICENOW_ERROR, USER);
    }
    if (response.code() == 404) {
      throw new ServiceNowException("404 Not found", SERVICENOW_ERROR, USER);
    }
    if (response.errorBody() == null) {
      throw new ServiceNowException(message + " : " + response.message(), SERVICENOW_ERROR, USER);
    }
    throw new ServiceNowException(response.errorBody().string(), SERVICENOW_ERROR, USER);
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
