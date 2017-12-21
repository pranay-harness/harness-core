package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.utils.JsonUtils;

@Data
@Builder
public class NewRelicDeploymentMarkerPayload {
  private Deployment deployment;

  @Data
  @Builder
  public static class Deployment {
    private Integer revision;
    private String description;
    private String changelog;
    private String user;
  }

  public static void main(String[] args) {
    NewRelicDeploymentMarkerPayload payload = JsonUtils.asObject("{\n"
            + "\"deployment\": {\n"
            + "    \"revision\": 40,\n"
            + "    \"description\": \"Harness Deployment via workflow todolist-newrelic-canary\" ,\n"
            + "    \"user\": \"todolist-newrelic-canary\" \n"
            + " }\n"
            + "}",
        NewRelicDeploymentMarkerPayload.class);

    System.out.println(payload);
  }
}
