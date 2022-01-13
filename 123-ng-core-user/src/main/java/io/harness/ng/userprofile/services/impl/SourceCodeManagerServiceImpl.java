/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.userprofile.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.entities.SourceCodeManager;
import io.harness.ng.userprofile.entities.SourceCodeManager.SourceCodeManagerMapper;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.repositories.ng.userprofile.spring.SourceCodeManagerRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
import com.hazelcast.util.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(PL)
@NoArgsConstructor
@AllArgsConstructor
public class SourceCodeManagerServiceImpl implements SourceCodeManagerService {
  @Inject SourceCodeManagerRepository sourceCodeManagerRepository;
  @Inject private Map<SCMType, SourceCodeManagerMapper> scmMapBinder;

  @Override
  public List<SourceCodeManagerDTO> get(String accountIdentifier) {
    Optional<String> userIdentifier = getUserIdentifier();
    return getInternal(userIdentifier, accountIdentifier);
  }

  @Override
  public List<SourceCodeManagerDTO> get(String userIdentifier, String accountIdentifier) {
    return getInternal(Optional.of(userIdentifier), accountIdentifier);
  }

  private List<SourceCodeManagerDTO> getInternal(Optional<String> userIdentifier, String accountIdentifier) {
    if (userIdentifier.isPresent()) {
      List<SourceCodeManagerDTO> sourceCodeManagerDTOS = new ArrayList<>();
      sourceCodeManagerRepository.findByUserIdentifierAndAccountIdentifier(userIdentifier.get(), accountIdentifier)
          .forEach(scm -> sourceCodeManagerDTOS.add(scmMapBinder.get(scm.getType()).toSCMDTO(scm)));
      return sourceCodeManagerDTOS;
    }
    return null;
  }

  @Override
  public SourceCodeManagerDTO save(SourceCodeManagerDTO sourceCodeManagerDTO) {
    Optional<String> userIdentifier = getUserIdentifier();
    if (userIdentifier.isPresent()) {
      SourceCodeManager sourceCodeManager = null;
      sourceCodeManagerDTO.setUserIdentifier(userIdentifier.get());
      try {
        sourceCodeManager = sourceCodeManagerRepository.save(
            scmMapBinder.get(sourceCodeManagerDTO.getType()).toSCMEntity(sourceCodeManagerDTO));
      } catch (DuplicateKeyException e) {
        throw new DuplicateFieldException(
            format("Source Code Manager with userId [%s], accountId [%s] and name [%s] already exists",
                userIdentifier.get(), sourceCodeManagerDTO.getAccountIdentifier(), sourceCodeManagerDTO.getName()));
      }
      return scmMapBinder.get(sourceCodeManager.getType()).toSCMDTO(sourceCodeManager);
    }
    return null;
  }

  @Override
  public SourceCodeManagerDTO update(String sourceCodeManagerIdentifier, SourceCodeManagerDTO sourceCodeManagerDTO) {
    Preconditions.checkNotNull(sourceCodeManagerIdentifier, "Source code manager identifier cannot be null");
    Optional<String> userIdentifier = getUserIdentifier();
    if (userIdentifier.isPresent()) {
      sourceCodeManagerDTO.setId(sourceCodeManagerIdentifier);
      Optional<SourceCodeManager> savedSCM = sourceCodeManagerRepository.findById(sourceCodeManagerDTO.getId());
      if (savedSCM.isPresent()) {
        SourceCodeManager toUpdateSCM =
            scmMapBinder.get(sourceCodeManagerDTO.getType()).toSCMEntity(sourceCodeManagerDTO);
        toUpdateSCM.setId(savedSCM.get().getId());

        try {
          toUpdateSCM = sourceCodeManagerRepository.save(toUpdateSCM);
        } catch (DuplicateKeyException e) {
          throw new DuplicateFieldException(
              format("Source Code Manager with userId [%s], accountId [%s] and name [%s] already exists",
                  userIdentifier.get(), sourceCodeManagerDTO.getAccountIdentifier(), sourceCodeManagerDTO.getName()));
        }
        return scmMapBinder.get(toUpdateSCM.getType()).toSCMDTO(toUpdateSCM);
      } else {
        throw new InvalidRequestException(
            format("Cannot find Source code manager with scm identifier [%s]", sourceCodeManagerDTO.getId()));
      }
    }
    return null;
  }

  @Override
  public boolean delete(String name, String accountIdentifier) {
    Optional<String> userIdentifier = getUserIdentifier();
    if (userIdentifier.isPresent()) {
      return sourceCodeManagerRepository.deleteByUserIdentifierAndNameAndAccountIdentifier(
                 userIdentifier.get(), name, accountIdentifier)
          > 0;
    }
    return false;
  }

  private Optional<String> getUserIdentifier() {
    Optional<String> userId = Optional.empty();
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      userId = Optional.of(SourcePrincipalContextBuilder.getSourcePrincipal().getName());
    }
    return userId;
  }
}
