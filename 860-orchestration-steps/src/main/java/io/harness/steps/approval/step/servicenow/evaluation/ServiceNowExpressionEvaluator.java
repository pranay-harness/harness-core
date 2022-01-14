package io.harness.steps.approval.step.servicenow.evaluation;

import io.harness.expression.EngineExpressionEvaluator;
import io.harness.servicenow.ServiceNowTicketNG;

public class ServiceNowExpressionEvaluator extends EngineExpressionEvaluator {
  public static final String TICKET_IDENTIFIER = "ticket";

  private final ServiceNowTicketNG serviceNowTicketNG;

  public ServiceNowExpressionEvaluator(ServiceNowTicketNG serviceNowTicketNG) {
    super(null);
    this.serviceNowTicketNG = serviceNowTicketNG;
  }

  @Override
  protected void initialize() {
    super.initialize();
    addToContext(TICKET_IDENTIFIER, serviceNowTicketNG.getFields());
  }
}
