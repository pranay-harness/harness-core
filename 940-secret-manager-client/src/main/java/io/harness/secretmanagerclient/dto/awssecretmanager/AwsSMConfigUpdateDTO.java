package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsSMConfigUpdateDTO extends SecretManagerConfigUpdateDTO {
  BaseAwsSMConfigDTO baseAwsSMConfigDTO;
}
