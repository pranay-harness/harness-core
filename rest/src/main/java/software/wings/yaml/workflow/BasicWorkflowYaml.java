package software.wings.yaml.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author rktummala on 11/1/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BasicWorkflowYaml extends WorkflowYaml {
  public static final class Builder extends WorkflowYaml.Builder {
    private Builder() {}

    @Override
    protected BasicWorkflowYaml getWorkflowYaml() {
      return new BasicWorkflowYaml();
    }

    public static Builder aYaml() {
      return new Builder();
    }
  }
}