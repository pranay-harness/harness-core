package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.core.Response;

@OwnedBy(CDP)
public interface AwsMarketPlaceApiHandler {
  Response processAWSMarktPlaceOrder(String token);
}
