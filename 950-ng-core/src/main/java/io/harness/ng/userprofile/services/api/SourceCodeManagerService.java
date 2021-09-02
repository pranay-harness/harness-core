package io.harness.ng.userprofile.services.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;

import java.util.List;

@OwnedBy(PL)
public interface SourceCodeManagerService {
  List<SourceCodeManagerDTO> get(String accountIdentifier);

  List<SourceCodeManagerDTO> get(String userIdentifier, String accountIdentifier);

  SourceCodeManagerDTO save(SourceCodeManagerDTO sourceCodeManagerDTO);

  SourceCodeManagerDTO update(String sourceCodeManagerIdentifier, SourceCodeManagerDTO sourceCodeManagerDTO);

  boolean delete(String scmIdentifier, String accountIdentifier);
}
