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

import com.google.inject.Inject;
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
    ModuleLicenseDTO trialLicense = licenseInterface.createTrialLicense(
        Edition.ENTERPRISE, accountIdentifier, LicenseType.TRIAL, startTrialRequestDTO.getModuleType());
    ModuleLicense savedEntity;
    try {
      savedEntity = moduleLicenseRepository.save(licenseObjectMapper.toEntity(trialLicense));
      // Send telemetry
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Trial license for moduleType [%s] already exists in account [%s]",
          startTrialRequestDTO.getModuleType(), accountIdentifier));
    }

    updateDefaultExperienceIfNull(accountIdentifier, startTrialRequestDTO.getModuleType());
    return licenseObjectMapper.toDTO(savedEntity);
  }

  private void updateDefaultExperienceIfNull(String accountIdentifier, ModuleType moduleType) {
    if (ModuleType.CI.equals(moduleType)) {
      accountService.updateDefaultExperienceIfNull(accountIdentifier, DefaultExperience.NG);
    }
  }
}
