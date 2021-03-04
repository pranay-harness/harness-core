package software.wings.verification.instana;

import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Yaml.
 */
@Data
@JsonPropertyOrder({"type", "harnessApiVersion"})
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class InstanaCVConfigurationYaml extends CVConfigurationYaml {
  private List<InstanaTagFilter> tagFilters;
}
