package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author rktummala on 09/07/2018
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

  private long expiryTime;
}
