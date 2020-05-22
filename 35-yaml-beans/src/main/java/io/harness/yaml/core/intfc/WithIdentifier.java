package io.harness.yaml.core.intfc;

import javax.validation.constraints.NotNull;

/**
 *  Any class that has identifier property should implement this interface
 */
public interface WithIdentifier {
  /**
   * Non-changeable identifier of the pipeline, can not contain spaces or special chars. REQUIRED
   * @return identifier
   */
  @NotNull String getIdentifier();
}
