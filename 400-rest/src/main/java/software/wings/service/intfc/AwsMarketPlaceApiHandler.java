/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.core.Response;

@OwnedBy(CDP)
public interface AwsMarketPlaceApiHandler {
  Response processAWSMarktPlaceOrder(String token);
}
