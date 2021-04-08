package io.harness.secretmanagerclient.dto.awskms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AwsKmsConfigUpdateDTO extends SecretManagerConfigUpdateDTO {
  BaseAwsKmsConfigDTO baseAwsKmsConfigDTO;
}
