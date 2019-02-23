package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author rktummala on 09/07/2018
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
