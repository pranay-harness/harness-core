package software.wings.api.pcf;

import io.harness.beans.SweepingOutput;

import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("swapRouteRollbackSweepingOutputPcf")
public class SwapRouteRollbackSweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "pcfSwapRouteRollbackSweepingOutput";
  private String uuid;
  private String name;
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;
  private List<String> tags;

  @Override
  public String getType() {
    return "swapRouteRollbackSweepingOutputPcf";
  }
}
