package io.harness.licensing.beans.transactions;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

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
      @JsonSubTypes.Type(value = CDLicenseTransactionDTO.class, name = "CD")
      , @JsonSubTypes.Type(value = CILicenseTransactionDTO.class, name = "CI"),
          @JsonSubTypes.Type(value = CELicenseTransactionDTO.class, name = "CE"),
          @JsonSubTypes.Type(value = CVLicenseTransactionDTO.class, name = "CV"),
          @JsonSubTypes.Type(value = CFLicenseTransactionDTO.class, name = "CF"),
    })
public class LicenseTransactionDTO {
  String uuid;
  String accountIdentifier;
  ModuleType moduleType;
  Edition edition;
  LicenseType licenseType;
  long startTime;
  long expiryTime;
  LicenseStatus status;
  Long createdAt;
  Long lastModifiedAt;
}
