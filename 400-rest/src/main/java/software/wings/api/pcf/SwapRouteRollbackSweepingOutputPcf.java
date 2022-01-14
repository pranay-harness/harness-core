package software.wings.api.pcf;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;
import io.harness.delegate.beans.pcf.CfRouteUpdateRequestConfigData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("swapRouteRollbackSweepingOutputPcf")
@TargetModule(HarnessModule._957_CG_BEANS)
public class SwapRouteRollbackSweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "pcfSwapRouteRollbackSweepingOutput";
  private String uuid;
  private String name;
  private CfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;
  private List<String> tags;

  @Override
  public String getType() {
    return "swapRouteRollbackSweepingOutputPcf";
  }
}
