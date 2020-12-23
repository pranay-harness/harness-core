package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.AGORODETKI;

import static software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType.INCIDENT;

import static org.junit.runners.Parameterized.Parameters;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.beans.yaml.ChangeContext;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.exception.IncompleteStateException;
import software.wings.yaml.workflow.StepYaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ServiceNowStepCompletionYamlValidatorTest {
  private final ChangeContext<StepYaml> changeContext;

  @Rule public final ExpectedException exception = ExpectedException.none();

  private final StepCompletionYamlValidator validator;

  public ServiceNowStepCompletionYamlValidatorTest(
      ChangeContext<StepYaml> changeContext, String message, Class<? extends Exception> expectedException) {
    this.changeContext = changeContext;
    this.validator = new ServiceNowStepCompletionYamlValidator();
    if (expectedException != null) {
      exception.expect(expectedException);
      exception.expectMessage(message);
    }
  }

  @Parameters
  public static Collection<Object[]> data() {
    ServiceNowCreateUpdateParams nullParams = null;

    ServiceNowCreateUpdateParams paramsWithoutActionType = new ServiceNowCreateUpdateParams();

    ServiceNowCreateUpdateParams paramsWithoutConnectorId = new ServiceNowCreateUpdateParams();
    paramsWithoutConnectorId.setAction(ServiceNowAction.CREATE);

    ServiceNowCreateUpdateParams createParamsWithoutTicketType = new ServiceNowCreateUpdateParams();
    createParamsWithoutTicketType.setAction(ServiceNowAction.CREATE);
    createParamsWithoutTicketType.setSnowConnectorId("connectorId");

    ServiceNowCreateUpdateParams createParamsWithoutFields = new ServiceNowCreateUpdateParams();
    createParamsWithoutFields.setAction(ServiceNowAction.CREATE);
    createParamsWithoutFields.setSnowConnectorId("connectorId");
    createParamsWithoutFields.setTicketType(INCIDENT.getDisplayName());

    ServiceNowCreateUpdateParams createParamsWithoutDescription = new ServiceNowCreateUpdateParams();
    createParamsWithoutDescription.setAction(ServiceNowAction.CREATE);
    createParamsWithoutDescription.setSnowConnectorId("connectorId");
    createParamsWithoutDescription.setTicketType(INCIDENT.getDisplayName());
    createParamsWithoutDescription.setFields(Collections.singletonMap(ServiceNowFields.SHORT_DESCRIPTION, "value"));

    ServiceNowCreateUpdateParams createParamsWithoutShortDescription = new ServiceNowCreateUpdateParams();
    createParamsWithoutShortDescription.setAction(ServiceNowAction.CREATE);
    createParamsWithoutShortDescription.setSnowConnectorId("connectorId");
    createParamsWithoutShortDescription.setTicketType(INCIDENT.getDisplayName());
    createParamsWithoutShortDescription.setFields(Collections.singletonMap(ServiceNowFields.DESCRIPTION, "value"));

    ServiceNowCreateUpdateParams updateParamsWithoutTicketType = new ServiceNowCreateUpdateParams();
    updateParamsWithoutTicketType.setAction(ServiceNowAction.UPDATE);
    updateParamsWithoutTicketType.setSnowConnectorId("connectorId");

    ServiceNowCreateUpdateParams updateParamsWithoutIssueNumber = new ServiceNowCreateUpdateParams();
    updateParamsWithoutIssueNumber.setAction(ServiceNowAction.UPDATE);
    updateParamsWithoutIssueNumber.setSnowConnectorId("connectorId");
    updateParamsWithoutIssueNumber.setTicketType(INCIDENT.getDisplayName());

    ServiceNowCreateUpdateParams importParamsWithoutTableName = new ServiceNowCreateUpdateParams();
    importParamsWithoutTableName.setAction(ServiceNowAction.IMPORT_SET);
    importParamsWithoutTableName.setSnowConnectorId("connectorId");

    ServiceNowCreateUpdateParams importParamsWithoutJsonBody = new ServiceNowCreateUpdateParams();
    importParamsWithoutJsonBody.setAction(ServiceNowAction.IMPORT_SET);
    importParamsWithoutJsonBody.setSnowConnectorId("connectorId");
    importParamsWithoutJsonBody.setImportSetTableName("tableName");
    return Arrays.asList(
        new Object[][] {{buildChangeContextWith(nullParams),
                            "\"serviceNowCreateUpdateParams\" could not be empty. Please provide the values.",
                            IncompleteStateException.class},
            {buildChangeContextWith(paramsWithoutActionType),
                String.format("\"action\" could not be null or empty. Please, provide valid value: %s",
                    Arrays.toString(ServiceNowAction.values())),
                IncompleteStateException.class},
            {buildChangeContextWith(paramsWithoutConnectorId), "\"snowConnectorId\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(createParamsWithoutTicketType), "\"ticketType\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(createParamsWithoutFields), "\"fields\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(createParamsWithoutDescription), "\"DESCRIPTION\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(createParamsWithoutShortDescription),
                "\"SHORT_DESCRIPTION\" could not be empty or null.", IncompleteStateException.class},
            {buildChangeContextWith(updateParamsWithoutTicketType), "\"ticketType\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(updateParamsWithoutIssueNumber), "\"issueNumber\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(importParamsWithoutTableName), "\"importSetTableName\" could not be empty or null.",
                IncompleteStateException.class},
            {buildChangeContextWith(importParamsWithoutJsonBody), "\"jsonBody\" could not be empty or null.",
                IncompleteStateException.class}});
  }

  private static ChangeContext buildChangeContextWith(ServiceNowCreateUpdateParams serviceNowCreateUpdateParams) {
    Map<String, Object> serviceNowCreateUpdateValues = JsonUtils.asMap(JsonUtils.asJson(serviceNowCreateUpdateParams));
    if (serviceNowCreateUpdateParams != null) {
      serviceNowCreateUpdateValues.put("fields", serviceNowCreateUpdateParams.fetchFields());
      serviceNowCreateUpdateValues.put("additionalFields", serviceNowCreateUpdateParams.fetchAdditionalFields());
    }
    Map<String, Object> properties =
        Collections.singletonMap("serviceNowCreateUpdateParams", serviceNowCreateUpdateValues);
    return ChangeContext.Builder.aChangeContext().withYaml(StepYaml.builder().properties(properties).build()).build();
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldValidateStepYaml() {
    validator.validate(changeContext);
  }
}
