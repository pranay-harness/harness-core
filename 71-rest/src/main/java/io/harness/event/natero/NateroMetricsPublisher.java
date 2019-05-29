package io.harness.event.natero;

import static javax.ws.rs.client.Entity.entity;

import io.harness.serializer.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Created by Pranjal on 05/26/2019
 */
@Slf4j
public class NateroMetricsPublisher {
  private Client client;
  private String EVENT_AUTH_KEY;
  private String EVENT_API_KEY;
  private String URL;

  public boolean NATERO_ENABLED;

  public boolean isNATERO_ENABLED() {
    return NATERO_ENABLED;
  }

  public NateroMetricsPublisher() {
    if (System.getenv().containsKey("NATERO_ENABLED") && System.getenv("NATERO_ENABLED").equals("true")) {
      NATERO_ENABLED = true;
      EVENT_AUTH_KEY = System.getenv("NATERO_EVENT_AUTH_KEY");
      EVENT_API_KEY = System.getenv("NATERO_EVENT_API_KEY");
      URL = "https://events.natero.com/v1/" + EVENT_AUTH_KEY + "/" + EVENT_API_KEY;
    }
    client = ClientBuilder.newBuilder().hostnameVerifier((s1, s2) -> true).register(MultiPartFeature.class).build();
  }

  public void publishNateroFeatureMetric(String accountId, String module, String feature, double value) {
    NateroFeaturePayload featurePayload = new NateroFeaturePayload();
    featurePayload.setAccountId(accountId);
    featurePayload.setUserId(accountId);
    featurePayload.setCreatedAt(System.currentTimeMillis());
    featurePayload.setAction("feature");
    featurePayload.setFeature(feature);
    featurePayload.setModule(module);
    featurePayload.setProduct(module);
    featurePayload.setTotal(value);

    WebTarget target = client.target(URL);

    Response response =
        getRequestBuilderWithAuthHeader(target).post(entity(featurePayload, MediaType.APPLICATION_JSON));

    if (response.getStatus() != Status.OK.getStatusCode()) {
      logger.error("Non-ok-status. Headers: {}", response.getHeaders());
    } else {
      logger.info(
          "Published Natero metrics for accountId : {}, module : {}, feature : {}, value : {}  JSON payload : {}",
          accountId, module, feature, value, JsonUtils.asJson(featurePayload));
    }
  }

  protected Builder getRequestBuilderWithAuthHeader(WebTarget target) {
    return target.request();
  }

  @Data
  private class NateroFeaturePayload {
    private String accountId;
    private String userId;
    private long createdAt;
    private String action;
    private String feature;
    private String module;
    private String product;
    private double total;
  }
}
