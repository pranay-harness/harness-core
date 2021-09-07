package io.harness.licensing.entities.modules;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.beans.modules.types.CDLicenseType;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "moduleLicenses", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.license.entities.module.CDModuleLicense")
public class CDModuleLicense extends ModuleLicense {
  CDLicenseType cdLicenseType;
  private Integer workloads;
  private Integer serviceInstances;
}
