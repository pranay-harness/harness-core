package io.harness.ng.core.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.ng.core.account.DefaultExperience;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "AccountDTO")
public class AccountDTO {
  @ApiModelProperty(required = true) @EntityIdentifier(allowBlank = false) String identifier;
  @ApiModelProperty(required = true) @NGEntityName String name;
  String companyName;
  String cluster;
  DefaultExperience defaultExperience;

  @Builder
  public AccountDTO(
      String identifier, String name, String companyName, String cluster, DefaultExperience defaultExperience) {
    this.identifier = identifier;
    this.name = name;
    this.companyName = companyName;
    this.cluster = cluster;
    this.defaultExperience = defaultExperience;
  }
}
