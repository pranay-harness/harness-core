package software.wings.verification.dynatrace;

import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Pranjal on 10/16/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DynaTraceCVServiceConfiguration extends CVConfiguration {
  private String serviceEntityId;

  @Override
  public CVConfiguration deepCopy() {
    DynaTraceCVServiceConfiguration clonedConfig = new DynaTraceCVServiceConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setServiceEntityId(this.serviceEntityId);
    return clonedConfig;
  }
}
