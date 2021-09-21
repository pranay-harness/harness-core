package io.harness.licensing.services;

import static io.harness.licensing.interfaces.ModuleLicenseImpl.TRIAL_DURATION;
import static io.harness.remote.client.RestClientUtils.getResponse;

import static java.lang.String.format;

import io.harness.ModuleType;
import io.harness.account.services.AccountService;
import io.harness.beans.EmbeddedUser;
import io.harness.ccm.license.CeLicenseInfoDTO;
import io.harness.ccm.license.remote.CeLicenseClient;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialDTO;
import io.harness.licensing.beans.response.CheckExpiryResultDTO;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.repositories.ModuleLicenseRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DefaultLicenseServiceImpl implements LicenseService {
  private final ModuleLicenseRepository moduleLicenseRepository;
  private final LicenseObjectConverter licenseObjectConverter;
  private final ModuleLicenseInterface licenseInterface;
  private final AccountService accountService;
  private final TelemetryReporter telemetryReporter;
  private final CeLicenseClient ceLicenseClient;

  static final String FAILED_OPERATION = "START_TRIAL_ATTEMPT_FAILED";
  static final String SUCCEED_START_FREE_OPERATION = "FREE_PLAN";
  static final String SUCCEED_START_TRIAL_OPERATION = "NEW_TRIAL";
  static final String SUCCEED_EXTEND_TRIAL_OPERATION = "EXTEND_TRIAL";
  static final String TRIAL_ENDED = "TRIAL_ENDED";

  @Override
  @Deprecated
  public ModuleLicenseDTO getModuleLicense(String accountIdentifier, ModuleType moduleType) {
    List<ModuleLicense> licenses =
        moduleLicenseRepository.findByAccountIdentifierAndModuleType(accountIdentifier, moduleType);
    if (licenses.isEmpty()) {
      log.debug(String.format(
          "ModuleLicense with ModuleType [%s] and accountIdentifier [%s] not found", moduleType, accountIdentifier));
      return null;
    }
    return licenseObjectConverter.toDTO(licenses.get(0));
  }

  @Override
  public List<ModuleLicenseDTO> getModuleLicenses(String accountIdentifier, ModuleType moduleType) {
    List<ModuleLicense> licenses =
        moduleLicenseRepository.findByAccountIdentifierAndModuleType(accountIdentifier, moduleType);
    return licenses.stream().map(licenseObjectConverter::<ModuleLicenseDTO>toDTO).collect(Collectors.toList());
  }

  @Override
  public AccountLicenseDTO getAccountLicense(String accountIdentifier) {
    List<ModuleLicense> licenses = moduleLicenseRepository.findByAccountIdentifier(accountIdentifier);
    AccountLicenseDTO dto = AccountLicenseDTO.builder().accountId(accountIdentifier).build();
    Map<ModuleType, ModuleLicenseDTO> licenseDTOMap =
        licenses.stream()
            .map(licenseObjectConverter::<ModuleLicenseDTO>toDTO)
            .collect(Collectors.toMap(ModuleLicenseDTO::getModuleType, l -> l, (existing, replacement) -> existing));
    dto.setModuleLicenses(licenseDTOMap);

    // For new structure
    Map<ModuleType, List<ModuleLicenseDTO>> allModuleLicenses =
        licenses.stream()
            .map(licenseObjectConverter::<ModuleLicenseDTO>toDTO)
            .collect(Collectors.groupingBy(ModuleLicenseDTO::getModuleType));
    dto.setAllModuleLicenses(allModuleLicenses);
    return dto;
  }

  @Override
  public ModuleLicenseDTO getModuleLicenseById(String identifier) {
    Optional<ModuleLicense> license = moduleLicenseRepository.findById(identifier);
    if (!license.isPresent()) {
      log.debug(String.format("ModuleLicense with identifier [%s] not found", identifier));
      return null;
    }
    return licenseObjectConverter.toDTO(license.get());
  }

  @Override
  public ModuleLicenseDTO createModuleLicense(ModuleLicenseDTO moduleLicense) {
    ModuleLicense license = licenseObjectConverter.toEntity(moduleLicense);
    // Validate entity
    ModuleLicense savedEntity;
    try {
      savedEntity = moduleLicenseRepository.save(license);
      // Send telemetry
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException("ModuleLicense already exists");
    }

    log.info("Created license for module [{}] in account [{}]", savedEntity.getModuleType(),
        savedEntity.getAccountIdentifier());
    return licenseObjectConverter.toDTO(savedEntity);
  }

  @Override
  public ModuleLicenseDTO updateModuleLicense(ModuleLicenseDTO moduleLicense) {
    ModuleLicense license = licenseObjectConverter.toEntity(moduleLicense);
    // validate the license
    Optional<ModuleLicense> existingEntityOptional = moduleLicenseRepository.findById(moduleLicense.getId());
    if (!existingEntityOptional.isPresent()) {
      throw new NotFoundException(String.format("ModuleLicense with identifier [%s] not found", moduleLicense.getId()));
    }

    ModuleLicense existedLicense = existingEntityOptional.get();
    license.setId(existedLicense.getId());
    license.setAccountIdentifier(existedLicense.getAccountIdentifier());
    license.setModuleType(existedLicense.getModuleType());
    ModuleLicense updatedLicense = moduleLicenseRepository.save(license);

    log.info("Updated license for module [{}] in account [{}]", updatedLicense.getModuleType(),
        updatedLicense.getAccountIdentifier());
    return licenseObjectConverter.toDTO(updatedLicense);
  }

  @Override
  public ModuleLicenseDTO startFreeLicense(String accountIdentifier, ModuleType moduleType) {
    ModuleLicenseDTO trialLicenseDTO = licenseInterface.generateFreeLicense(accountIdentifier, moduleType);

    AccountDTO accountDTO = accountService.getAccount(accountIdentifier);
    if (accountDTO == null) {
      throw new InvalidRequestException(String.format("Account [%s] doesn't exists", accountIdentifier));
    }

    List<ModuleLicense> existing =
        moduleLicenseRepository.findByAccountIdentifierAndModuleType(accountIdentifier, moduleType);
    if (!existing.isEmpty()) {
      String cause = format("Account [%s] already have licenses for moduleType [%s]", accountIdentifier, moduleType);
      sendFailedTelemetryEvents(accountIdentifier, moduleType, null, Edition.FREE, cause);
      throw new InvalidRequestException(cause);
    }

    ModuleLicense trialLicense = licenseObjectConverter.toEntity(trialLicenseDTO);
    trialLicense.setCreatedBy(EmbeddedUser.builder().email(getEmailFromPrincipal()).build());
    ModuleLicense savedEntity = moduleLicenseRepository.save(trialLicense);
    sendSucceedTelemetryEvents(SUCCEED_START_FREE_OPERATION, savedEntity, accountIdentifier);

    log.info("Free license for module [{}] is started in account [{}]", moduleType, accountIdentifier);

    accountService.updateDefaultExperienceIfApplicable(accountIdentifier, DefaultExperience.NG);
    return licenseObjectConverter.toDTO(savedEntity);
  }

  @Override
  public ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialDTO startTrialRequestDTO) {
    Edition edition = startTrialRequestDTO.getEdition();
    LicenseType licenseType = LicenseType.TRIAL;
    ModuleLicenseDTO trialLicenseDTO =
        licenseInterface.generateTrialLicense(edition, accountIdentifier, startTrialRequestDTO.getModuleType());

    AccountDTO accountDTO = accountService.getAccount(accountIdentifier);
    if (accountDTO == null) {
      throw new InvalidRequestException(String.format("Account [%s] doesn't exists", accountIdentifier));
    }

    List<ModuleLicense> existing = moduleLicenseRepository.findByAccountIdentifierAndModuleType(
        accountIdentifier, startTrialRequestDTO.getModuleType());
    if (!existing.isEmpty()) {
      String cause = format("Account [%s] already have licenses for moduleType [%s]", accountIdentifier,
          startTrialRequestDTO.getModuleType());
      sendFailedTelemetryEvents(accountIdentifier, startTrialRequestDTO.getModuleType(), licenseType, edition, cause);
      throw new InvalidRequestException(cause);
    }

    ModuleLicense trialLicense = licenseObjectConverter.toEntity(trialLicenseDTO);
    trialLicense.setCreatedBy(EmbeddedUser.builder().email(getEmailFromPrincipal()).build());
    ModuleLicense savedEntity = moduleLicenseRepository.save(trialLicense);
    sendSucceedTelemetryEvents(SUCCEED_START_TRIAL_OPERATION, savedEntity, accountIdentifier);

    log.info("Trial license for module [{}] is started in account [{}]", startTrialRequestDTO.getModuleType(),
        accountIdentifier);

    accountService.updateDefaultExperienceIfApplicable(accountIdentifier, DefaultExperience.NG);
    startTrialInCGIfCE(savedEntity);
    return licenseObjectConverter.toDTO(savedEntity);
  }

  @Override
  public ModuleLicenseDTO extendTrialLicense(String accountIdentifier, StartTrialDTO startTrialRequestDTO) {
    LicenseType licenseType = LicenseType.TRIAL;
    ModuleType moduleType = startTrialRequestDTO.getModuleType();

    List<ModuleLicense> existing =
        moduleLicenseRepository.findByAccountIdentifierAndModuleType(accountIdentifier, moduleType);

    if (existing.isEmpty()) {
      String cause =
          format("Trial license for moduleType [%s] hasn't started in account [%s]", moduleType, accountIdentifier);
      log.error(cause);
      sendFailedTelemetryEvents(accountIdentifier, moduleType, licenseType, null, cause);
      throw new InvalidRequestException(cause);
    }

    if (isNotEligibleToExtend(existing)) {
      String cause = format("Can not extend trial for account [%s]. Please contact sales.", accountIdentifier);
      sendFailedTelemetryEvents(accountIdentifier, moduleType, licenseType, null, cause);
      throw new InvalidRequestException(cause);
    }

    ModuleLicense existingModuleLicense = existing.get(0);

    ModuleLicenseDTO trialLicense =
        licenseInterface.generateTrialLicense(existingModuleLicense.getEdition(), accountIdentifier, moduleType);
    ModuleLicense savedEntity = moduleLicenseRepository.save(licenseObjectConverter.toEntity(trialLicense));

    sendSucceedTelemetryEvents(SUCCEED_EXTEND_TRIAL_OPERATION, savedEntity, accountIdentifier);
    syncLicenseChangeToCGForCE(savedEntity);
    log.info("Trial license for module [{}] is extended in account [{}]", moduleType, accountIdentifier);
    return licenseObjectConverter.toDTO(savedEntity);
  }

  @Override
  public CheckExpiryResultDTO checkExpiry(String accountIdentifier) {
    List<ModuleLicense> licenses = moduleLicenseRepository.findByAccountIdentifier(accountIdentifier);
    long currentTime = Instant.now().toEpochMilli();
    long maxExpiryTime = 0;
    boolean isPaidOrFree = false;
    for (ModuleLicense moduleLicense : licenses) {
      if (Edition.FREE.equals(moduleLicense.getEdition())) {
        isPaidOrFree = true;
        continue;
      }
      if (LicenseType.PAID.equals(moduleLicense.getLicenseType())) {
        isPaidOrFree = true;
      }

      if (moduleLicense.checkExpiry(currentTime)) {
        if (moduleLicense.isActive()) {
          // In case need to expire license
          moduleLicense.setStatus(LicenseStatus.EXPIRED);
          moduleLicenseRepository.save(moduleLicense);

          if (LicenseType.TRIAL.equals(moduleLicense.getLicenseType())) {
            sendTrialEndEvents(moduleLicense, moduleLicense.getCreatedBy());
          }
        }
      }

      if (maxExpiryTime < moduleLicense.getExpiryTime()) {
        maxExpiryTime = moduleLicense.getExpiryTime();
      }
    }

    log.info("Check for module licenses under account {} complete, used {} milliseconds", accountIdentifier,
        Instant.now().toEpochMilli() - currentTime);
    return CheckExpiryResultDTO.builder()
        .shouldDelete(!isPaidOrFree && (maxExpiryTime <= currentTime))
        .expiryTime(maxExpiryTime)
        .build();
  }

  @Override
  public void softDelete(String accountIdentifier) {}

  private void sendSucceedTelemetryEvents(String eventName, ModuleLicense moduleLicense, String accountIdentifier) {
    String email = getEmailFromPrincipal();
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", email);
    properties.put("module", moduleLicense.getModuleType());

    if (moduleLicense.getLicenseType() != null) {
      properties.put("licenseType", moduleLicense.getLicenseType());
    }

    if (moduleLicense.getEdition() != null) {
      properties.put("plan", moduleLicense.getEdition());
    }

    properties.put("platform", "NG");
    properties.put("startTime", String.valueOf(moduleLicense.getStartTime()));
    properties.put("duration", TRIAL_DURATION);
    properties.put("licenseStatus", moduleLicense.getStatus());
    telemetryReporter.sendTrackEvent(eventName, properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build(), Category.SIGN_UP);

    HashMap<String, Object> groupProperties = new HashMap<>();
    String moduleType = moduleLicense.getModuleType().name();

    if (moduleLicense.getEdition() != null) {
      groupProperties.put(format("%s%s", moduleType, "LicenseEdition"), moduleLicense.getEdition());
    }

    if (moduleLicense.getLicenseType() != null) {
      groupProperties.put(format("%s%s", moduleType, "LicenseType"), moduleLicense.getLicenseType());
    }

    groupProperties.put(format("%s%s", moduleType, "LicenseStartTime"), moduleLicense.getStartTime());
    groupProperties.put(format("%s%s", moduleType, "LicenseDuration"), TRIAL_DURATION);
    groupProperties.put(format("%s%s", moduleType, "LicenseStatus"), moduleLicense.getStatus());
    telemetryReporter.sendGroupEvent(accountIdentifier, groupProperties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build());
  }

  private void sendFailedTelemetryEvents(
      String accountIdentifier, ModuleType moduleType, LicenseType licenseType, Edition edition, String cause) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("reason", cause);
    properties.put("module", moduleType);

    if (licenseType != null) {
      properties.put("licenseType", licenseType);
    }

    if (edition != null) {
      properties.put("plan", edition);
    }

    telemetryReporter.sendTrackEvent(FAILED_OPERATION, properties, null, Category.SIGN_UP);
  }

  private void sendTrialEndEvents(ModuleLicense moduleLicense, EmbeddedUser user) {
    HashMap<String, Object> properties = new HashMap<>();
    String email = "unknown";
    if (user != null) {
      email = user.getEmail();
    }
    properties.put("email", email);
    properties.put("module", moduleLicense.getModuleType());
    properties.put("licenseType", moduleLicense.getLicenseType());
    properties.put("plan", moduleLicense.getEdition());
    properties.put("platform", "NG");
    properties.put("startTime", String.valueOf(moduleLicense.getStartTime()));
    properties.put("endTime", String.valueOf(moduleLicense.getExpiryTime()));
    properties.put("duration", TRIAL_DURATION);
    telemetryReporter.sendTrackEvent(TRIAL_ENDED, email, moduleLicense.getAccountIdentifier(), properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build(), Category.SIGN_UP);
  }

  private String getEmailFromPrincipal() {
    Principal principal = SourcePrincipalContextBuilder.getSourcePrincipal();
    String email = "";
    if (principal instanceof UserPrincipal) {
      email = ((UserPrincipal) principal).getEmail();
    }
    return email;
  }

  private boolean isNotEligibleToExtend(List<ModuleLicense> moduleLicenses) {
    if (moduleLicenses.size() > 1) {
      return true;
    }

    ModuleLicense moduleLicense = moduleLicenses.get(0);
    Duration duration = Duration.ofMillis(Instant.now().toEpochMilli() - moduleLicense.getExpiryTime());
    return duration.toMillis() <= 0 || duration.toDays() > 14 || LicenseType.PAID.equals(moduleLicense.getLicenseType())
        || Edition.FREE.equals(moduleLicense.getEdition());
  }

  private void startTrialInCGIfCE(ModuleLicense moduleLicense) {
    if (ModuleType.CE.equals(moduleLicense.getModuleType())) {
      try {
        getResponse(ceLicenseClient.createCeTrial(CeLicenseInfoDTO.builder()
                                                      .accountId(moduleLicense.getAccountIdentifier())
                                                      .expiryTime(moduleLicense.getExpiryTime())
                                                      .build()));
      } catch (Exception e) {
        log.error("Unable to sync trial start in CG CCM", e);
      }
    }
  }

  private void syncLicenseChangeToCGForCE(ModuleLicense moduleLicense) {
    if (ModuleType.CE.equals(moduleLicense.getModuleType())) {
      try {
        getResponse(ceLicenseClient.updateCeLicense(CeLicenseInfoDTO.builder()
                                                        .accountId(moduleLicense.getAccountIdentifier())
                                                        .expiryTime(moduleLicense.getExpiryTime())
                                                        .build()));
      } catch (Exception e) {
        log.error("Unable to sync license info in CG CCM", e);
      }
    }
  }
}
