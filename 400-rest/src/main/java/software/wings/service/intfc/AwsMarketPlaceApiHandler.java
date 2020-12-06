package software.wings.service.intfc;

import javax.ws.rs.core.Response;

public interface AwsMarketPlaceApiHandler {
  Response processAWSMarktPlaceOrder(String token);
}
