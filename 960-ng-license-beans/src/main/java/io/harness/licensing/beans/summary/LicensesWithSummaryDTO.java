package io.harness.licensing.beans.summary;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.GTM)
@SuperBuilder
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "moduleType", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = CDLicenseSummaryDTO.class, name = "CD")
      , @JsonSubTypes.Type(value = CILicenseSummaryDTO.class, name = "CI"),
          @JsonSubTypes.Type(value = CELicenseSummaryDTO.class, name = "CE"),
          @JsonSubTypes.Type(value = CVLicenseSummaryDTO.class, name = "CV"),
          @JsonSubTypes.Type(value = CFLicenseSummaryDTO.class, name = "CF"),
    })
public abstract class LicensesWithSummaryDTO {
  Edition edition;
  LicenseType licenseType;
  ModuleType moduleType;
  long maxExpiryTime;
}
