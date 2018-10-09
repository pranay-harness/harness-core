package software.wings.verification;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import javax.validation.constraints.NotNull;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */

@Entity(value = "verificationServiceConfigurations")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CVConfiguration extends Base {
  @NotNull private String envId;
  @NotNull private String serviceId;
  private boolean enabled24x7;

  public CVConfiguration(@NotNull String appId, @NotNull String envId, @NotNull String serviceId, boolean enabled24x7) {
    this.appId = appId;
    this.envId = envId;
    this.serviceId = serviceId;
    this.enabled24x7 = enabled24x7;
  }
}
