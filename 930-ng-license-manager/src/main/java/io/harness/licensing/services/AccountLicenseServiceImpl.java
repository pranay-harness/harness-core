package io.harness.licensing.services;

import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import static java.lang.String.format;

import io.harness.account.services.AccountService;
import io.harness.exception.DuplicateFieldException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.beans.modules.AccountLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.StartTrialRequestDTO;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.interfaces.ModuleLicenseInterface;
import io.harness.licensing.mappers.AccountLicenseMapper;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.repositories.AccountLicenseRepository;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;
import io.harness.telemetry.Category;
import io.harness.telemetry.TelemetryReporter;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class AccountLicenseServiceImpl implements AccountLicenseService {
  private final AccountLicenseRepository accountLicenseRepository;
  private final LicenseObjectConverter licenseObjectMapper;
  private final AccountLicenseMapper accountLicenseMapper;
  private final ModuleLicenseInterface licenseInterface;
  private final AccountService accountService;
  private final TelemetryReporter telemetryReporter;

  static final String FAILED_OPERATION = "Start trial attempt failed";
  static final String SUCCEED_OPERATION = "Start trial succeed";

  @Override
  public ModuleLicenseDTO getModuleLicense(String accountIdentifier, ModuleType moduleType) {
    AccountLicense accountLicense = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (accountLicense == null || accountLicense.getModuleLicenses() == null
        || accountLicense.getModuleLicenses().get(moduleType) == null) {
      log.debug(String.format(
          "ModuleLicense with ModuleType [%s] and accountIdentifier [%s] not found", moduleType, accountIdentifier));
      return null;
    }
    return licenseObjectMapper.toDTO(accountLicense.getModuleLicenses().get(moduleType));
  }

  @Override
  public AccountLicenseDTO getAccountLicense(String accountIdentifier) {
    AccountLicense licenses = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (licenses == null) {
      log.debug(String.format("AccountLicense with accountIdentifier [%s] not found", accountIdentifier));
      return null;
    }
    return accountLicenseMapper.toDTO(licenses);
  }

  @Override
  public AccountLicenseDTO getAccountLicenseById(String identifier) {
    Optional<AccountLicense> license = accountLicenseRepository.findById(identifier);
    if (!license.isPresent()) {
      log.debug(String.format("ModuleLicense with identifier [%s] not found", identifier));
      return null;
    }
    return accountLicenseMapper.toDTO(license.get());
  }

  @Override
  public AccountLicenseDTO createAccountLicense(AccountLicenseDTO accountLicenseDTO) {
    AccountLicense license = accountLicenseMapper.toEntity(accountLicenseDTO);

    // Validate entity
    AccountLicense savedEntity;
    try {
      // reset uuid to avoid doing update instead of create
      license.setUuid(null);
      license.setLicenseCheckIteration(0L);
      validateAccountLicense(license);
      savedEntity = accountLicenseRepository.save(license);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException("AccountLicense already exists");
    }
    // Send telemetry

    return accountLicenseMapper.toDTO(savedEntity);
  }

  @Override
  public AccountLicenseDTO updateAccountLicense(AccountLicenseDTO accountLicenseDTO) {
    AccountLicense license = accountLicenseMapper.toEntity(accountLicenseDTO);
    // validate the license
    Optional<AccountLicense> existingEntityOptional = accountLicenseRepository.findById(license.getUuid());
    if (!existingEntityOptional.isPresent()) {
      throw new NotFoundException(String.format("ModuleLicense with identifier [%s] not found", license.getUuid()));
    }

    license.setCreatedAt(existingEntityOptional.get().getCreatedAt());
    license.setLicenseCheckIteration(0L);
    license.getModuleLicenses().values().forEach(
        l -> l.setAccountIdentifier(existingEntityOptional.get().getAccountIdentifier()));
    validateAccountLicense(license);
    AccountLicense accountLicense = accountLicenseRepository.save(license);
    return accountLicenseMapper.toDTO(accountLicense);
  }

  @Override
  public ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO) {
    Edition edition = Edition.ENTERPRISE;
    LicenseType licenseType = LicenseType.TRIAL;
    ModuleType moduleType = startTrialRequestDTO.getModuleType();
    ModuleLicenseDTO trialLicenseDTO =
        licenseInterface.generateTrialLicense(edition, accountIdentifier, licenseType, moduleType);
    ModuleLicense trialLicense = licenseObjectMapper.toEntity(trialLicenseDTO);

    AccountLicense accountLicense;
    accountLicense = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (accountLicense != null && accountLicense.getModuleLicenses() != null) {
      ModuleLicense existingLicense = accountLicense.getModuleLicenses().get(moduleType);
      if (existingLicense != null) {
        String cause = format("Trial license for moduleType [%s] already exists in account [%s]",
            startTrialRequestDTO.getModuleType(), accountIdentifier);
        sendFailedTelemetryEvents(accountIdentifier, startTrialRequestDTO.getModuleType(), licenseType, edition, cause);
        throw new DuplicateFieldException(cause);
      } else {
        // add moduleType if module licenses map exists
        accountLicense.getModuleLicenses().put(moduleType, trialLicense);
      }
    } else {
      // first time start trial, create account license along with trial module license
      accountLicense = AccountLicense.builder()
                           .accountIdentifier(accountIdentifier)
                           .moduleLicenses(ImmutableMap.of(moduleType, trialLicense))
                           .allInactive(false)
                           .build();
    }
    validateAccountLicense(accountLicense);
    AccountLicense savedEntity = accountLicenseRepository.save(accountLicense);
    sendSucceedTelemetryEvents(savedEntity.getModuleLicenses().get(moduleType), accountIdentifier);

    log.info("Trial license for module [{}] is started in account [{}]", startTrialRequestDTO.getModuleType(),
        accountIdentifier);
    accountService.updateDefaultExperienceIfApplicable(accountIdentifier, DefaultExperience.NG);
    return licenseObjectMapper.toDTO(savedEntity.getModuleLicenses().get(moduleType));
  }

  @Override
  public boolean checkNGLicensesAllInactive(String accountIdentifier) {
    AccountLicense accountLicense = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (accountLicense == null) {
      return true;
    }
    return accountLicense.isAllInactive();
  }

  @Override
  public void softDelete(String accountIdentifier) {
    // TODO implement soft delete account logic here
  }

  private void validateAccountLicense(AccountLicense accountLicense) {
    accountLicense.getModuleLicenses().values().forEach(m -> setIdForTransactionIfNull(m));
  }

  private void setIdForTransactionIfNull(ModuleLicense moduleLicense) {
    List<LicenseTransaction> transactions = moduleLicense.getTransactions();
    if (transactions != null) {
      transactions.stream().filter(t -> t.getUuid() == null).forEach(t -> t.setUuid(ObjectId.get().toHexString()));
    }
  }

  private void sendSucceedTelemetryEvents(ModuleLicense moduleLicense, String accountIdentifier) {
    Principal principal = SourcePrincipalContextBuilder.getSourcePrincipal();
    String email = "";
    if (principal instanceof UserPrincipal) {
      email = ((UserPrincipal) principal).getEmail();
    }

    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", email);
    properties.put("module", moduleLicense.getModuleType());
    properties.put("licenseType", moduleLicense.getLicenseType());
    properties.put("plan", moduleLicense.getEdition());
    properties.put("platform", "NG");
    properties.put("startTime", moduleLicense.getTransactions().get(0).getStartTime());
    properties.put("duration", TRIAL_DURATION);
    properties.put("licenseStatus", moduleLicense.getStatus());
    telemetryReporter.sendTrackEvent(SUCCEED_OPERATION, properties, null, Category.SIGN_UP);

    HashMap<String, Object> groupProperties = new HashMap<>();
    String moduleType = moduleLicense.getModuleType().name();
    groupProperties.put(format("%s%s", moduleType, "LicenseEdition"), moduleLicense.getEdition());
    groupProperties.put(format("%s%s", moduleType, "LicenseType"), moduleLicense.getLicenseType());
    groupProperties.put(
        format("%s%s", moduleType, "LicenseStartTinme"), moduleLicense.getTransactions().get(0).getStartTime());
    groupProperties.put(format("%s%s", moduleType, "LicenseDuration"), TRIAL_DURATION);
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
