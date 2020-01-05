/**
 *
 */

package software.wings.api;

import com.google.inject.Inject;

import io.harness.context.ContextElementType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.NameValuePair;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class PhaseElement.
 *
 * @author Rishi
 */

@Data
@Builder
public class PhaseElement implements ContextElement {
  public static final String PHASE_PARAM = "PHASE_PARAM";

  @Inject @Transient private transient ArtifactService artifactService;

  private String uuid;
  private String phaseName;
  private ServiceElement serviceElement;
  private String appId;
  @Getter(AccessLevel.NONE) private String infraMappingId;
  private String deploymentType;
  private String phaseNameForRollback;
  @Builder.Default private List<NameValuePair> variableOverrides = new ArrayList<>();
  private String rollbackArtifactId;
  private String infraDefinitionId;
  private String workflowExecutionId;
  private boolean rollback;
  private boolean onDemandRollback;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getName() {
    return PHASE_PARAM;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    Map<String, Object> map = new HashMap<>();
    map.put(SERVICE, serviceElement);

    if (rollbackArtifactId != null) {
      Artifact artifact = artifactService.getWithSource(rollbackArtifactId);
      map.put(ARTIFACT, artifact);
    }
    return map;
  }

  public String getPhaseExecutionIdForSweepingOutput() {
    return workflowExecutionId + uuid + phaseName;
  }
}
