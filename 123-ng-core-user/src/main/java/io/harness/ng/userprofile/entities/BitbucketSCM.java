/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.userprofile.entities;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.bitbucketconnector.BitbucketAuthentication;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketDTOToEntity;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketEntityToDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.ng.userprofile.commons.BitbucketSCMDTO;
import io.harness.ng.userprofile.commons.SCMType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "BitBucketSCMKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.ng.userprofile.entities.BitbucketSCM")
public class BitbucketSCM extends SourceCodeManager {
  GitAuthType authType;
  BitbucketAuthentication authenticationDetails;

  @Override
  public SCMType getType() {
    return SCMType.BITBUCKET;
  }

  public static class BitbucketSCMMapper extends SourceCodeManagerMapper<BitbucketSCMDTO, BitbucketSCM> {
    @Override
    public BitbucketSCM toSCMEntity(BitbucketSCMDTO sourceCodeManagerDTO) {
      BitbucketSCM bitbucketSCM = BitbucketSCM.builder()
                                      .authType(sourceCodeManagerDTO.getAuthentication().getAuthType())
                                      .authenticationDetails(BitbucketDTOToEntity.buildAuthenticationDetails(
                                          sourceCodeManagerDTO.getAuthentication().getAuthType(),
                                          sourceCodeManagerDTO.getAuthentication().getCredentials()))
                                      .build();
      setCommonFieldsEntity(bitbucketSCM, sourceCodeManagerDTO);
      return bitbucketSCM;
    }

    @Override
    public BitbucketSCMDTO toSCMDTO(BitbucketSCM sourceCodeManager) {
      BitbucketSCMDTO bitbucketSCMDTO =
          BitbucketSCMDTO.builder()
              .authentication(BitbucketEntityToDTO.buildBitbucketAuthentication(
                  sourceCodeManager.getAuthType(), sourceCodeManager.getAuthenticationDetails()))
              .build();
      setCommonFieldsDTO(sourceCodeManager, bitbucketSCMDTO);
      return bitbucketSCMDTO;
    }
  }
}
