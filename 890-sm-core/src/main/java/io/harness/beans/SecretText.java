package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataParams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "SecretTextKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretText extends HarnessSecret {
  private String value;
  private String path;
  private Set<EncryptedDataParams> parameters;
  public boolean isInlineSecret() {
    return hasNone(path) && !isParameterizedSecret();
  }
  public boolean isReferencedSecret() {
    return hasSome(path);
  }
  public boolean isParameterizedSecret() {
    return parameters != null;
  }
}
