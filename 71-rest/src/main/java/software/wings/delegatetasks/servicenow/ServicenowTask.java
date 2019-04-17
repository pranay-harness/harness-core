package software.wings.delegatetasks.servicenow;

import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl.getBaseUrl;
import static software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl.getRetrofit;
import static software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl.handleResponse;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
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
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.helpers.ext.servicenow.ServiceNowRestClient;
import software.wings.service.intfc.security.EncryptionService;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
      default:
        String errorMsg = "Invalid ServiceNow delegate Task Action " + parameters.getAction();
        logger.error(errorMsg);
        throw new WingsException(ErrorCode.SERVICENOW_ERROR, WingsException.USER).addParam("message", errorMsg);
    }
  }

  private ResponseData createServiceNowTicket(ServiceNowTaskParameters parameters) {
    ServiceNowConfig config = parameters.getServiceNowConfig();

    Map<String, String> body = new HashMap<>();
    for (Entry<ServiceNowFields, String> entry : parameters.getFields().entrySet()) {
      body.put(entry.getKey().getJsonBodyName(), entry.getValue());
    }

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

  private ResponseData updateServiceNowTicket(ServiceNowTaskParameters parameters) {
    ServiceNowConfig config = parameters.getServiceNowConfig();
    Map<String, String> body = new HashMap<>();
    for (Entry<ServiceNowFields, String> entry : parameters.getFields().entrySet()) {
      body.put(entry.getKey().getJsonBodyName(), entry.getValue());
    }

    if (parameters.getIssueId() == null && parameters.getIssueNumber() != null) {
      String query = "number=" + parameters.getIssueNumber();
      final Call<JsonNode> request =
          getRestClient(parameters)
              .getIssueId(Credentials.basic(config.getUsername(), new String(config.getPassword())),
                  parameters.getTicketType().toString().toLowerCase(), query);
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
            String issueId = issueObj.get("sys_id").asText();
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
      String errorMsg = "Error while updating serviceNow ticket: ";
      throw new WingsException(SERVICENOW_ERROR, USER, e).addParam("message", errorMsg + ExceptionUtils.getMessage(e));
    }
  }

  private ServiceNowRestClient getRestClient(ServiceNowTaskParameters taskParameters) {
    ServiceNowConfig config = taskParameters.getServiceNowConfig();
    encryptionService.decrypt(config, taskParameters.getEncryptionDetails());
    return getRetrofit(getBaseUrl(config), JacksonConverterFactory.create()).create(ServiceNowRestClient.class);
  }
}
