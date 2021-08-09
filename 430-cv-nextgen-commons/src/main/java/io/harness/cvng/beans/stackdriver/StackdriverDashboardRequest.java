package io.harness.cvng.beans.stackdriver;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeName("STACKDRIVER_DASHBOARD_LIST")
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CV)
public class StackdriverDashboardRequest extends StackdriverRequest {
  public static final String DSL =
      StackdriverDashboardRequest.readDSL("stackdriver-dashboards.datacollection", StackdriverDashboardRequest.class);

  @Override
  public String getDSL() {
    return DSL;
  }
}
