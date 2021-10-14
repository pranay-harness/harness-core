package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@OwnedBy(CDC)
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._957_CG_BEANS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShellScriptApprovalParams {
  @Getter @Setter private String scriptString;

  /* Retry Interval in Milliseconds*/
  @Getter @Setter private Integer retryInterval;
  private List<String> delegateSelectors;

  public void setDelegateSelectors(List<String> delegateSelectors) {
    this.delegateSelectors = delegateSelectors;
  }

  public List<String> fetchDelegateSelectors() {
    return this.delegateSelectors;
  }
}
