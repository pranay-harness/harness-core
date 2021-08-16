package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

/**
 * @author rktummala on 09/07/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldNameConstants(innerTypeName = "LicenseInfoKeys")
@TargetModule(HarnessModule._957_CG_BEANS)
@OwnedBy(PL)
public class LicenseInfo implements Serializable {
  private static final long serialVersionUID = 2006711824734916828L;

  /**
   * @see AccountType
   */
  private String accountType;

  /**
   * @see AccountStatus
   */
  private String accountStatus;

  @JsonIgnore private transient int expireAfterDays;

  private long expiryTime;

  private int licenseUnits;

  @JsonIgnore
  public int getExpireAfterDays() {
    return expireAfterDays;
  }

  @JsonIgnore
  public void setExpireAfterDays(int expireAfterDays) {
    this.expireAfterDays = expireAfterDays;
  }
}
