package io.harness.ng.core.smtp;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.NgSmtpDTO;
import io.harness.ng.core.dto.ValidationResultDTO;

import java.io.IOException;

@OwnedBy(PL)
public interface SmtpNgService {
  NgSmtpDTO saveSmtpSettings(NgSmtpDTO variable) throws IOException;
  ValidationResultDTO validateSmtpSettings(String name, String accountId) throws IOException;
  NgSmtpDTO updateSmtpSettings(NgSmtpDTO variable) throws IOException;
  ValidationResultDTO validateConnectivitySmtpSettings(
      String identifier, String accountId, String to, String subject, String body) throws IOException;
  Boolean deleteSmtpSettings(String id) throws IOException;
  NgSmtpDTO getSmtpSettings(String accountId) throws IOException;
}
