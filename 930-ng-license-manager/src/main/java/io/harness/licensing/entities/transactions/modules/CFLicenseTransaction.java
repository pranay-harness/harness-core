package io.harness.licensing.entities.transactions.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.entities.transactions.LicenseTransaction;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.GTM)
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Persistent
@Document("licenseTransactions")
@TypeAlias("io.harness.license.entities.transactions.modules.CFLicenseTransaction")
public class CFLicenseTransaction extends LicenseTransaction {
  private Integer featureFlagUnit;
  private Long clientMAU;
}
