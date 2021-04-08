package io.harness.secretmanagerclient.dto.awskms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsKmsConfigDTO extends SecretManagerConfigDTO {
  BaseAwsKmsConfigDTO baseAwsKmsConfigDTO;
}
