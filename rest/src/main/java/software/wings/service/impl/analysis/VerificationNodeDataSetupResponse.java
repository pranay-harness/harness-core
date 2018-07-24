package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 7/24/18.
 */
@Data
@Builder
public class VerificationNodeDataSetupResponse {
  private boolean providerReachable;
  private VerificationLoadResponse loadResponse;
  private Object dataForNode;

  @Data
  @Builder
  public static class VerificationLoadResponse {
    private boolean isLoadPresent;
    private Object loadResponse;
  }
}
