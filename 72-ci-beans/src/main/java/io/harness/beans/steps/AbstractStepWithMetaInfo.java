package io.harness.beans.steps;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.state.io.StepParameters;
import io.harness.yaml.core.intfc.StepInfo;
import io.harness.yaml.core.nonyaml.WithNonYamlInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * CI Step which stores state parameters and metadata for creating advisers and facilitators
 */
@Data
@NoArgsConstructor
public abstract class AbstractStepWithMetaInfo implements StepInfo, StepParameters, WithNonYamlInfo<TypeInfo> {
  public static final int DEFAULT_RETRY = 0;
  public static final int MIN_RETRY = 0;
  public static final int MAX_RETRY = 5;
  public static final int DEFAULT_TIMEOUT = 120;
  public static final int MIN_TIMEOUT = 1;
  public static final int MAX_TIMEOUT = 999;

  @NotNull private String type;
  @NotNull private String identifier;
  private String name;
  private List<String> dependencies;

  @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry = DEFAULT_RETRY;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) private int timeout = DEFAULT_TIMEOUT; // in minutes

  private StepMetadata stepMetadata =
      StepMetadata.builder().retry(DEFAULT_RETRY).timeout(DEFAULT_TIMEOUT).uuid(generateUuid()).build();

  public AbstractStepWithMetaInfo(
      String type, String identifier, String name, List<String> dependencies, Integer retry, Integer timeout) {
    this.type = type;
    this.identifier = identifier;
    this.name = name;
    this.dependencies = dependencies;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
  }
}
