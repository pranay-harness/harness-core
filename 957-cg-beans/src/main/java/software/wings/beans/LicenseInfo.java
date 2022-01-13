/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

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
