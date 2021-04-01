package io.harness.ng.userprofile.services.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.userprofile.commons.SCMType;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.entities.SourceCodeManager;
import io.harness.ng.userprofile.entities.SourceCodeManager.SourceCodeManagerMapper;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.repositories.ng.userprofile.spring.SourceCodeManagerRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
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
        throw new DuplicateFieldException(
            format("Source Code Manager with name [%s] already exists", sourceCodeManagerDTO.getName()));
      }
      return scmMapBinder.get(sourceCodeManager.getType()).toSCMDTO(sourceCodeManager);
    }
    return null;
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
