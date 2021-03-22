package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.NotSaved;

/**
 * Created by anubhaw on 11/17/16.
 */
@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineStage {
  private String name;
  private boolean parallel;
  private List<PipelineStageElement> pipelineStageElements = new ArrayList<>();
  private transient boolean valid = true;
  private transient String validationMessage;
  private transient boolean looped;
  private transient String loopedVarName;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PipelineStageElement {
    private String uuid;
    private String name;
    private String type;
    private int parallelIndex;
    private Map<String, Object> properties = new HashMap<>();
    private Map<String, String> workflowVariables = new HashMap<>();
    private RuntimeInputsConfig runtimeInputsConfig;

    // Remove this once UI moves away from it
    @NotSaved private boolean disable;
    private String disableAssertion;

    private transient boolean valid = true;
    private transient String validationMessage;

    public boolean checkDisableAssertion() {
      return disableAssertion != null && disableAssertion.equals("true");
    }
  }
}
