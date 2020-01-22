package software.wings.delegatetasks.servicenow;

import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl.getBaseUrl;
import static software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl.getRetrofit;
import static software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl.handleResponse;
import static software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType.CHANGE_TASK;

import com.google.gson.Gson;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.api.ServiceNowExecutionData;
import software.wings.api.ServiceNowImportSetResponse;
import software.wings.api.ServiceNowImportSetResult;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.servicenow.ServiceNowRestClient;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class ServicenowTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;

  public ServicenowTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    return run((ServiceNowTaskParameters) parameters);
  }

  private ResponseData run(ServiceNowTaskParameters parameters) {
    ServiceNowAction action = parameters.getAction();

    ResponseData responseData = null;

    logger.info("Executing ServiceNowTask. Action: {}, IssueNumber: {}", action, parameters.getIssueNumber());

    switch (parameters.getAction()) {
      case CREATE:
        return createServiceNowTicket(parameters);

      case UPDATE:
        return updateServiceNowTicket(parameters);
      case IMPORT_SET:
        return createImportSets(parameters);
      default:
        String errorMsg = "Invalid ServiceNow delegate Task Action " + parameters.getAction();
        logger.error(errorMsg);
        throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER).addParam("message", errorMsg);
    }
  }

  private ResponseData createServiceNowTicket(ServiceNowTaskParameters parameters) {
    ServiceNowConfig config = parameters.getServiceNowConfig();

    Map<String, String> body = new HashMap<>();
    parameters.getFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key.getJsonBodyName(), value);
      }
    });
    parameters.getAdditionalFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key, value);
      }
    });

    final Call<JsonNode> request;
    request = getRestClient(parameters)
                  .createTicket(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                      parameters.getTicketType().toString().toLowerCase(), "all", "number,sys_id", body);

    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to create ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      String issueNumber = responseObj.get("number").get("display_value").asText();
      String issueId = responseObj.get("sys_id").get("display_value").asText();
      String issueUrl = getBaseUrl(config) + "nav_to.do?uri=/" + parameters.getTicketType().toString().toLowerCase()
          + ".do?sys_id=" + issueId;
      String responseMsg = "Created ServiceNow ticket: " + issueNumber;
      return ServiceNowExecutionData.builder()
          .issueNumber(issueNumber)
          .issueId(issueId)
          .ticketType(parameters.getTicketType())
          .issueUrl(issueUrl)
          .responseMsg(responseMsg)
          .executionStatus(ExecutionStatus.SUCCESS)
          .build();
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      String errorMsg = "Error while creating serviceNow ticket: ";
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  private ResponseData createImportSets(ServiceNowTaskParameters parameters) {
    Gson gson = new Gson();
    Map body = gson.fromJson(parameters.getJsonBody(), HashMap.class);
    ServiceNowConfig config = parameters.getServiceNowConfig();
    final Call<JsonNode> request;
    request = getRestClient(parameters)
                  .createImportSet(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                      parameters.getImportSetTableName(), "all", body);

    Response<JsonNode> apiResponse = null;
    try {
      apiResponse = request.execute();
      logger.info("Response received from serviceNow: {}", apiResponse);
      handleResponse(apiResponse, "Failed to create ServiceNow ticket");
      String importSetNumber = apiResponse.body().get("import_set").textValue();
      ObjectMapper mapper = new ObjectMapper();

      ServiceNowImportSetResponse response =
          mapper.readValue(apiResponse.body().toString(), ServiceNowImportSetResponse.class);
      List<String> transformationValues = new ArrayList<>();

      for (ServiceNowImportSetResult result : response.getResult()) {
        transformationValues.add(result.getDisplayValue());
      }
      Collections.sort(transformationValues);

      String responseMsg = "Created import Set : " + importSetNumber;
      return ServiceNowExecutionData.builder()
          .ticketType(parameters.getTicketType())
          .transformationDetails(response)
          .transformationValues(transformationValues)
          .responseMsg(responseMsg)
          .executionStatus(ExecutionStatus.SUCCESS)
          .build();
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      String errorMsg = "Error while creating import set: ";
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  private ResponseData updateServiceNowTicket(ServiceNowTaskParameters parameters) {
    if (parameters.getTicketType() == CHANGE_TASK && parameters.isUpdateMultiple()) {
      return updateAllChangeTaskTickets(parameters);
    }

    ServiceNowConfig config = parameters.getServiceNowConfig();
    Map<String, String> body = new HashMap<>();

    parameters.getFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key.getJsonBodyName(), value);
      }
    });
    parameters.getAdditionalFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key, value);
      }
    });

    if (parameters.getIssueId() == null && parameters.getIssueNumber() != null) {
      setIssueIdFromIssueNumber(parameters, config);
    }

    final Call<JsonNode> request;
    request = getRestClient(parameters)
                  .updateTicket(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                      parameters.getTicketType().toString().toLowerCase(), parameters.getIssueId(), "all",
                      "number,sys_id", body);

    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to update ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      String issueNumber = responseObj.get("number").get("display_value").asText();
      String issueId = responseObj.get("sys_id").get("display_value").asText();
      String issueUrl = getBaseUrl(config) + "nav_to.do?uri=/" + parameters.getTicketType().toString().toLowerCase()
          + ".do?sys_id=" + issueId;
      String responseMsg = "Updated ServiceNow ticket: " + issueNumber;
      return ServiceNowExecutionData.builder()
          .issueNumber(issueNumber)
          .issueId(issueId)
          .issueUrl(issueUrl)
          .ticketType(parameters.getTicketType())
          .responseMsg(responseMsg)
          .executionStatus(ExecutionStatus.SUCCESS)
          .build();
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      String errorMsg = "Error while updating serviceNow ticket: " + parameters.getIssueNumber();
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  private ResponseData updateAllChangeTaskTickets(ServiceNowTaskParameters parameters) {
    if (!parameters.getFields().containsKey(ServiceNowFields.CHANGE_REQUEST_NUMBER)
        || EmptyPredicate.isEmpty(parameters.getFields().get(ServiceNowFields.CHANGE_REQUEST_NUMBER))) {
      throw new WingsException(SERVICENOW_ERROR, USER)
          .addParam("message", "Change Request Number is required to update change tasks");
    }

    List<String> changeTaskIdsToUpdate = fetchChangeTasksFromCR(parameters);
    String changeRequestNumber = parameters.getFields().get(ServiceNowFields.CHANGE_REQUEST_NUMBER);
    if (EmptyPredicate.isEmpty(changeTaskIdsToUpdate)) {
      return ServiceNowExecutionData.builder()
          .issueNumber(changeRequestNumber)
          .ticketType(parameters.getTicketType())
          .responseMsg("No change tasks to update for: " + changeRequestNumber)
          .executionStatus(ExecutionStatus.SUCCESS)
          .build();
    }
    Map<String, String> body = new HashMap<>();
    parameters.getFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key.getJsonBodyName(), value);
      }
    });
    parameters.getAdditionalFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key, value);
      }
    });
    body.remove(ServiceNowFields.CHANGE_REQUEST_NUMBER.getJsonBodyName());
    body.remove(ServiceNowFields.CHANGE_TASK_TYPE.getJsonBodyName());
    parameters.getAdditionalFields().forEach(body::put);

    ServiceNowConfig config = parameters.getServiceNowConfig();

    Response<JsonNode> response = null;
    List<String> updateChangeTaskNumbers = new ArrayList<>();
    List<String> updatedCjhangeTaskUrls = new ArrayList<>();
    Call<JsonNode> request;

    for (String changeTaskId : changeTaskIdsToUpdate) {
      try {
        request = getRestClient(parameters)
                      .updateTicket(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                          CHANGE_TASK.toString().toLowerCase(), changeTaskId, "all", "number,sys_id", body);

        response = request.execute();
        logger.info("Response received from serviceNow: {}", response);
        handleResponse(response, "Failed to update ServiceNow ticket");
        JsonNode responseObj = response.body().get("result");
        String issueNumber = responseObj.get("number").get("display_value").asText();
        String issueId = responseObj.get("sys_id").get("display_value").asText();
        String issueUrl = getBaseUrl(config) + "nav_to.do?uri=/" + parameters.getTicketType().toString().toLowerCase()
            + ".do?sys_id=" + issueId;

        logger.info("Successfully updated ticket : " + issueNumber);
        updateChangeTaskNumbers.add(issueNumber);
        updatedCjhangeTaskUrls.add(issueUrl);
      } catch (WingsException we) {
        throw we;
      } catch (Exception e) {
        String errorMsg = "Error while updating serviceNow task: " + changeTaskId;
        throw new WingsException(SERVICENOW_ERROR, USER, e)
            .addParam("message", errorMsg + ExceptionUtils.getMessage(e));
      }
    }
    return ServiceNowExecutionData.builder()
        .issueNumber(parameters.getFields().get(ServiceNowFields.CHANGE_REQUEST_NUMBER))
        .issueUrl(updatedCjhangeTaskUrls.toString().replaceAll("[\\[\\]]", ""))
        .ticketType(parameters.getTicketType())
        .responseMsg("Updated Service Now tasks " + updateChangeTaskNumbers.toString().replaceAll("[\\[\\]]", ""))
        .executionStatus(ExecutionStatus.SUCCESS)
        .build();
  }

  private List<String> fetchChangeTasksFromCR(ServiceNowTaskParameters parameters) {
    ServiceNowConfig config = parameters.getServiceNowConfig();
    final Call<JsonNode> request;
    List<String> changeTaskIds = new ArrayList<>();
    String changeRequestNumber = parameters.getFields().get(ServiceNowFields.CHANGE_REQUEST_NUMBER);

    request = getRestClient(parameters)
                  .fetchChangeTasksFromCR(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                      parameters.getTicketType().toString().toLowerCase(), "number,sys_id,change_task_type",
                      "change_request.number=" + changeRequestNumber);

    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to update ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        for (JsonNode changeTaskObj : responseObj) {
          if (parameters.getFields().containsKey(ServiceNowFields.CHANGE_TASK_TYPE)
              && EmptyPredicate.isNotEmpty(parameters.getFields().get(ServiceNowFields.CHANGE_TASK_TYPE))) {
            if (parameters.getFields()
                    .get(ServiceNowFields.CHANGE_TASK_TYPE)
                    .equalsIgnoreCase(changeTaskObj.get("change_task_type").textValue())) {
              changeTaskIds.add(changeTaskObj.get("sys_id").textValue());
            }
          } else {
            // update all ticket types
            changeTaskIds.add(changeTaskObj.get("sys_id").textValue());
          }
        }
        return changeTaskIds;
      } else {
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam("message", "Response for fetching changeTasks is not an array. Response: " + response);
      }

    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      String errorMsg = "Error while fetching change tasks for : " + changeRequestNumber;
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  private void setIssueIdFromIssueNumber(ServiceNowTaskParameters parameters, ServiceNowConfig config) {
    String query = "number=" + parameters.getIssueNumber();
    final Call<JsonNode> request =
        getRestClient(parameters)
            .getIssue(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                parameters.getTicketType().toString().toLowerCase(), query, "all");
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      logger.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to fetch IssueId : " + parameters.getIssueNumber() + " from serviceNow");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        if (responseObj.size() > 1) {
          String errorMsg =
              "Multiple issues found for " + parameters.getIssueNumber() + "Please enter unique issueNumber";
          throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", errorMsg);
        }
        JsonNode issueObj = responseObj.get(0);
        if (issueObj != null) {
          String issueId = issueObj.get("sys_id").get("display_value").asText();
          parameters.setIssueId(issueId);
        } else {
          String errorMsg = "Error in fetching issue " + parameters.getIssueNumber() + " .Issue does not exist";
          throw new WingsException(SERVICENOW_ERROR, USER).addParam("message", errorMsg);
        }
      } else {
        throw new WingsException(SERVICENOW_ERROR, USER)
            .addParam(
                "message", "Failed to fetch issueNumber " + parameters.getIssueNumber() + "response: " + response);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String errorMsg = "Error in fetching issueNumber " + parameters.getIssueNumber();
      throw new WingsException(SERVICENOW_ERROR, USER, e)
          .addParam("message", errorMsg + " " + ExceptionUtils.getMessage(e));
    }
  }

  private ServiceNowRestClient getRestClient(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    encryptionService.decrypt(config, taskParameters.getEncryptionDetails());
    return getRetrofit(getBaseUrl(config), JacksonConverterFactory.create()).create(ServiceNowRestClient.class);
  }
}
