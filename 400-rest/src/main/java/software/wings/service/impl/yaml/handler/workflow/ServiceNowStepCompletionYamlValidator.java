package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.serializer.JsonUtils;

import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.beans.yaml.ChangeContext;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.exception.IncompleteStateException;
import software.wings.yaml.workflow.StepYaml;

import java.util.Arrays;
import java.util.Map;

public class ServiceNowStepCompletionYamlValidator implements StepCompletionYamlValidator {
  private static final String SERVICE_NOW_ACTION = "action";

  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    Map<String, Object> snowParams =
        (Map<String, Object>) changeContext.getYaml().getProperties().get("serviceNowCreateUpdateParams");
    validateParamsAreInPlace(snowParams);
    validateActionType(snowParams);
    ServiceNowCreateUpdateParams serviceNowCreateUpdateParams =
        JsonUtils.asObject(JsonUtils.asJson(snowParams), ServiceNowCreateUpdateParams.class);
    validateConnector(serviceNowCreateUpdateParams);
    validateStructureForGivenActionType(serviceNowCreateUpdateParams);
  }

  private void validateParamsAreInPlace(Map<String, Object> snowParams) {
    if (isEmpty(snowParams)) {
      throw new IncompleteStateException(
          "\"serviceNowCreateUpdateParams\" could not be empty. Please provide the values.");
    }
  }

  private void validateStructureForGivenActionType(ServiceNowCreateUpdateParams serviceNowCreateUpdateParams) {
    switch (serviceNowCreateUpdateParams.getAction()) {
      case CREATE:
        validateCreate(serviceNowCreateUpdateParams);
        break;
      case UPDATE:
        validateUpdate(serviceNowCreateUpdateParams);
        break;
      case IMPORT_SET:
        validateImportSet(serviceNowCreateUpdateParams);
        break;
      default:
        throw new IncompleteStateException("Provided serviceNowAction is not supported. ");
    }
  }

  private void validateConnector(ServiceNowCreateUpdateParams serviceNowCreateUpdateParams) {
    if (isBlank(serviceNowCreateUpdateParams.getSnowConnectorId())) {
      throw new IncompleteStateException("\"snowConnectorId\" could not be empty or null.");
    }
  }

  private void validateImportSet(ServiceNowCreateUpdateParams serviceNowCreateUpdateParams) {
    if (isBlank(serviceNowCreateUpdateParams.getImportSetTableName())) {
      throw new IncompleteStateException("\"importSetTableName\" could not be empty or null.");
    }
    if (isBlank(serviceNowCreateUpdateParams.getJsonBody())) {
      throw new IncompleteStateException("\"jsonBody\" could not be empty or null.");
    }
  }

  private void validateUpdate(ServiceNowCreateUpdateParams serviceNowCreateUpdateParams) {
    if (isBlank(serviceNowCreateUpdateParams.getTicketType())) {
      throw new IncompleteStateException("\"ticketType\" could not be empty or null.");
    }
    if (isBlank(serviceNowCreateUpdateParams.getIssueNumber())) {
      throw new IncompleteStateException("\"issueNumber\" could not be empty or null.");
    }
  }

  private void validateCreate(ServiceNowCreateUpdateParams serviceNowCreateUpdateParams) {
    if (isBlank(serviceNowCreateUpdateParams.getTicketType())) {
      throw new IncompleteStateException("\"ticketType\" could not be empty or null.");
    }
    if (isEmpty(serviceNowCreateUpdateParams.fetchFields())) {
      throw new IncompleteStateException("\"fields\" could not be empty or null.");
    }
    if (isBlank(serviceNowCreateUpdateParams.fetchFields().get(ServiceNowFields.DESCRIPTION))) {
      throw new IncompleteStateException("\"DESCRIPTION\" could not be empty or null.");
    }
    if (isBlank(serviceNowCreateUpdateParams.fetchFields().get(ServiceNowFields.SHORT_DESCRIPTION))) {
      throw new IncompleteStateException("\"SHORT_DESCRIPTION\" could not be empty or null.");
    }
  }

  private void validateActionType(Map<String, Object> snowParams) {
    String serviceNowAction = (String) snowParams.get(SERVICE_NOW_ACTION);
    if (isBlank(serviceNowAction)) {
      throw new IncompleteStateException(
          String.format("\"action\" could not be null or empty. Please, provide valid value: %s",
              Arrays.toString(ServiceNowAction.values())));
    }
    try {
      ServiceNowAction.valueOf(serviceNowAction);
    } catch (IllegalArgumentException e) {
      throw new IncompleteStateException(
          String.format("Invalid value %s provided for \"action\". Please, provide valid value: %s", serviceNowAction,
              Arrays.toString(ServiceNowAction.values())));
    }
  }
}
