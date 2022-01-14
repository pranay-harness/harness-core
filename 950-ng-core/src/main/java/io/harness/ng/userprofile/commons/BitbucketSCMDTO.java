package io.harness.ng.userprofile.commons;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketAuthenticationDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@JsonTypeName("BITBUCKET")
@Data
@Schema(name = "BitbucketSCM", description = "This Contains details of the Bitbucket Source Code Manager")
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BitbucketSCMDTO extends SourceCodeManagerDTO {
  @JsonProperty("authentication") BitbucketAuthenticationDTO authentication;

  @Override
  public SCMType getType() {
    return SCMType.BITBUCKET;
  }
}
