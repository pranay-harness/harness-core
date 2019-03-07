package software.wings.beans.governance;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.HarnessExportableEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

/**
 * @author rktummala on 02/04/19
 */
@JsonInclude(NON_EMPTY)
@Entity(value = "governanceConfig", noClassnameStored = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@HarnessExportableEntity
public class GovernanceConfig extends Base {
  @Indexed private String accountId;
  private boolean deploymentFreeze;

  @Builder
  public GovernanceConfig(String accountId, boolean deploymentFreeze) {
    this.accountId = accountId;
    this.deploymentFreeze = deploymentFreeze;
  }
}
