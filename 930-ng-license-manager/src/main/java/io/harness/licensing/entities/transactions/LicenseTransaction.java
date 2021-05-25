package io.harness.licensing.entities.transactions;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.GTM)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "LicenseTransactionKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn(DbAliases.NG_MANAGER)
@Document
@Persistent
public abstract class LicenseTransaction implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id protected String uuid;
  @Trimmed @NotEmpty protected String accountIdentifier;
  @NotEmpty protected ModuleType moduleType;
  @NotEmpty protected Edition edition;
  @NotEmpty protected LicenseType licenseType;
  @NotEmpty protected long startTime;
  @NotEmpty protected long expiryTime;
  @NotEmpty protected LicenseStatus status;
  @CreatedBy protected EmbeddedUser createdBy;
  @LastModifiedBy protected EmbeddedUser lastUpdatedBy;
  @CreatedDate protected Long createdAt;
  @LastModifiedDate protected Long lastUpdatedAt;

  public boolean checkExpiry(long currentTime) {
    return currentTime >= expiryTime;
  }

  public boolean isActive() {
    return LicenseStatus.ACTIVE.equals(status);
  }

  public abstract LicenseTransaction makeTemplateCopy();
}
