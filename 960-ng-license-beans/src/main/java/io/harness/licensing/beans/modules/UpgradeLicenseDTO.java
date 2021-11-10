package io.harness.licensing.beans.modules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "Upgrade License", description = "This is the module which will be upgraded")
public class UpgradeLicenseDTO {
    @NotNull
    ModuleType moduleType;
    @NotNull
    Edition edition;
    @NotNull
    LicenseType licenseType;
}
