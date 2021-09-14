/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.demo;

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
