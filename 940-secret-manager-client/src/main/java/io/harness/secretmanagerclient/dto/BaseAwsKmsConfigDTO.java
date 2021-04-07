package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.*;
import lombok.experimental.SuperBuilder;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(exclude = {"add fields here"})//TODO: Shashank: Add fields here
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseAwsKmsConfigDTO {
  String accessKey;
  String secretKey;
  String kmsArn;
  String region;
  boolean isDefault;
}
