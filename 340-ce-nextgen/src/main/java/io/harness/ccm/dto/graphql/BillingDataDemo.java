package io.harness.ccm.dto.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.Value;

@Data
@Value(staticConstructor = "of")
@OwnedBy(CE)
public class BillingDataDemo {
  String instanceid;
  String instancename;
  Double billingamount;
  Long starttime;
}
