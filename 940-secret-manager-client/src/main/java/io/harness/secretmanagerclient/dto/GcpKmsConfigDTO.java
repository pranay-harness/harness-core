package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(exclude = {"credentials"})
@FieldNameConstants(innerTypeName = "GcpKmsConfigDTOKeys")
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcpKmsConfigDTO extends SecretManagerConfigDTO {
  String projectId;
  String region;
  String keyRing;
  String keyName;
  char[] credentials;
  Set<String> delegateSelectors;
}
