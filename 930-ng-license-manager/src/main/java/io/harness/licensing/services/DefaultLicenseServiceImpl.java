package io.harness.licensing.services;

import static java.lang.String.format;

import io.harness.account.services.AccountService;
import io.harness.exception.DuplicateFieldException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicensesDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.LicenseObjectMapper;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.repositories.ModuleLicenseRepository;
import io.harness.telemetry.Category;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
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
  private final LicenseObjectMapper licenseObjectMapper;
  private final ModuleLicenseInterface licenseInterface;
  private final AccountService accountService;
  private final TelemetryReporter telemetryReporter;

  static final String FAILED_OPERATION = "Start trial attempt failed";
  static final String SUCCEED_OPERATION = "Start trial succeed";

  @Override
  public ModuleLicenseDTO getModuleLicense(String accountIdentifier, ModuleType moduleType) {
    ModuleLicense license = moduleLicenseRepository.findByAccountIdentifierAndModuleType(accountIdentifier, moduleType);
    if (license == null) {
      log.debug(String.format(
          "ModuleLicense with ModuleType [%s] and accountIdentifier [%s] not found", moduleType, accountIdentifier));
      return null;
    }
    return licenseObjectMapper.toDTO(license);
  }

  @Override
  public AccountLicensesDTO getAccountLicense(String accountIdentifier) {
    List<ModuleLicense> licenses = moduleLicenseRepository.findByAccountIdentifier(accountIdentifier);
    Map<ModuleType, ModuleLicenseDTO> licenseDTOMap =
        licenses.stream()
            .map(licenseObjectMapper::toDTO)
            .collect(Collectors.toMap(ModuleLicenseDTO::getModuleType, l -> l));
    return AccountLicensesDTO.builder().accountId(accountIdentifier).moduleLicenses(licenseDTOMap).build();
  }

  @Override
  public ModuleLicenseDTO getModuleLicenseById(String identifier) {
    Optional<ModuleLicense> license = moduleLicenseRepository.findById(identifier);
    if (!license.isPresent()) {
      log.debug(String.format("ModuleLicense with identifier [%s] not found", identifier));
      return null;
    }
    return licenseObjectMapper.toDTO(license.get());
  }

  @Override
  public ModuleLicenseDTO createModuleLicense(ModuleLicenseDTO moduleLicense) {
    ModuleLicense license = licenseObjectMapper.toEntity(moduleLicense);
    // Validate entity
    ModuleLicense savedEntity;
    try {
      savedEntity = moduleLicenseRepository.save(license);
      // Send telemetry
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException("ModuleLicense already exists");
    }
    return licenseObjectMapper.toDTO(savedEntity);
  }

  @Override
  public ModuleLicenseDTO updateModuleLicense(ModuleLicenseDTO moduleLicense) {
    ModuleLicense license = licenseObjectMapper.toEntity(moduleLicense);
    // validate the license
    Optional<ModuleLicense> existingEntityOptional = moduleLicenseRepository.findById(moduleLicense.getId());
    if (!existingEntityOptional.isPresent()) {
      throw new NotFoundException(String.format("ModuleLicense with identifier [%s] not found", moduleLicense.getId()));
    }

    ModuleLicense existedLicense = existingEntityOptional.get();
    license.setId(existedLicense.getId());
    license.setAccountIdentifier(existedLicense.getAccountIdentifier());
    license.setCreatedAt(existedLicense.getCreatedAt());
    license.setCreatedBy(existedLicense.getCreatedBy());
    license.setModuleType(existedLicense.getModuleType());
    ModuleLicense updatedLicense = moduleLicenseRepository.save(license);
    return licenseObjectMapper.toDTO(updatedLicense);
  }

  @Override
  public ModuleLicenseDTO deleteModuleLicense(String id, String accountId) {
    Optional<ModuleLicense> existedLicense = moduleLicenseRepository.findById(id);
    if (!existedLicense.isPresent()) {
      throw new NotFoundException(String.format("ModuleLicense with identifier [%s] not found", id));
    }

    ModuleLicense moduleLicense = existedLicense.get();
    moduleLicense.setStatus(LicenseStatus.DELETED);
    // Delete process on corresponded microservice
    moduleLicenseRepository.save(moduleLicense);
    // Send Telemetry
    return licenseObjectMapper.toDTO(moduleLicense);
  }

  @Override
  public ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO) {
    Edition edition = Edition.ENTERPRISE;
    LicenseType licenseType = LicenseType.TRIAL;
    ModuleLicenseDTO trialLicense = licenseInterface.generateTrialLicense(
        edition, accountIdentifier, licenseType, startTrialRequestDTO.getModuleType());
    ModuleLicense savedEntity;
    try {
      savedEntity = moduleLicenseRepository.save(licenseObjectMapper.toEntity(trialLicense));
      sendSucceedTelemetryEvents(savedEntity, accountIdentifier);
    } catch (DuplicateKeyException ex) {
      String cause = format("Trial license for moduleType [%s] already exists in account [%s]",
          startTrialRequestDTO.getModuleType(), accountIdentifier);
      sendFailedTelemetryEvents(accountIdentifier, startTrialRequestDTO.getModuleType(), licenseType, edition, cause);
      throw new DuplicateFieldException(cause);
    }

    updateDefaultExperienceIfNull(accountIdentifier, startTrialRequestDTO.getModuleType());
    return licenseObjectMapper.toDTO(savedEntity);
  }

  private void updateDefaultExperienceIfNull(String accountIdentifier, ModuleType moduleType) {
    if (ModuleType.CI.equals(moduleType)) {
      accountService.updateDefaultExperienceIfNull(accountIdentifier, DefaultExperience.NG);
    }
  }

  private void sendSucceedTelemetryEvents(ModuleLicense moduleLicense, String accountIdentifier) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("module", moduleLicense.getModuleType());
    properties.put("licenseType", moduleLicense.getLicenseType());
    properties.put("licenseEdition", moduleLicense.getEdition());
    properties.put("startTime", moduleLicense.getStartTime());
    properties.put("expiryTime", moduleLicense.getExpiryTime());
    properties.put("licenseStatus", moduleLicense.getStatus());
    telemetryReporter.sendTrackEvent(SUCCEED_OPERATION, properties, null, Category.SIGN_UP);

    HashMap<String, Object> groupProperties = new HashMap<>();
    String moduleType = moduleLicense.getModuleType().name();
    groupProperties.put(format("%s%s", moduleType, "LicenseEdition"), moduleLicense.getEdition());
    groupProperties.put(format("%s%s", moduleType, "LicenseType"), moduleLicense.getLicenseType());
    groupProperties.put(format("%s%s", moduleType, "LicenseStartTinme"), moduleLicense.getStartTime());
    groupProperties.put(format("%s%s", moduleType, "LicenseExpiryTinme"), moduleLicense.getExpiryTime());
    groupProperties.put(format("%s%s", moduleType, "LicenseStatus"), moduleLicense.getStatus());
    telemetryReporter.sendGroupEvent(accountIdentifier, groupProperties, null);
  }

  private void sendFailedTelemetryEvents(
      String accountIdentifier, ModuleType moduleType, LicenseType licenseType, Edition edition, String cause) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("reason", cause);
    properties.put("module", moduleType);
    properties.put("licenseType", licenseType);
    properties.put("licenseEdition", edition);
    telemetryReporter.sendTrackEvent(FAILED_OPERATION, properties, null, Category.SIGN_UP);
  }
}
