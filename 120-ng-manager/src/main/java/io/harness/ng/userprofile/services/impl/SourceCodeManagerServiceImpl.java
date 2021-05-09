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
  public List<SourceCodeManagerDTO> get() {
    Optional<String> userIdentifier = getUserIdentifier();
    return getInternal(userIdentifier);
  }

  @Override
  public List<SourceCodeManagerDTO> get(String userIdentifier) {
    return getInternal(Optional.of(userIdentifier));
  }

  private List<SourceCodeManagerDTO> getInternal(Optional<String> userIdentifier) {
    if (userIdentifier.isPresent()) {
      List<SourceCodeManagerDTO> sourceCodeManagerDTOS = new ArrayList<>();
      sourceCodeManagerRepository.findByUserIdentifier(userIdentifier.get())
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
        throw new DuplicateFieldException(format("Source Code Manager with userId [%s], name [%s] already exists",
            userIdentifier.get(), sourceCodeManagerDTO.getName()));
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
          throw new DuplicateFieldException(format("Source Code Manager with userId [%s], name [%s] already exists",
              userIdentifier.get(), sourceCodeManagerDTO.getName()));
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
  public boolean delete(String name) {
    Optional<String> userIdentifier = getUserIdentifier();
    if (userIdentifier.isPresent()) {
      return sourceCodeManagerRepository.deleteByUserIdentifierAndName(userIdentifier.get(), name) > 0;
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
