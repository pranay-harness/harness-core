package io.harness.licensing.services;

import static io.harness.licensing.interfaces.ModuleLicenseInterfaceImpl.TRIAL_DURATION;

import static java.lang.String.format;

import io.harness.account.services.AccountService;
import io.harness.beans.EmbeddedUser;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
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
import io.harness.licensing.scheduler.AccountLicenseCheckHandler;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.repositories.AccountLicenseRepository;
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
import java.time.temporal.ChronoUnit;
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
  private final AccountLicenseCheckHandler accountLicenseCheckHandler;

  static final String FAILED_OPERATION = "Start trial attempt failed";
  static final String SUCCEED_START_TRIAL_OPERATION = "NEW_TRIAL";
  static final String SUCCEED_EXTEND_TRIAL_OPERATION = "EXTEND_TRIAL";

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
      license.setLicenseCheckIteration(Instant.now().toEpochMilli());
      verifyTransactionsAlignment(license);
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

    verifyTransactionsAlignment(license);
    license.setCreatedAt(existingEntityOptional.get().getCreatedAt());
    license.setLicenseCheckIteration(Instant.now().toEpochMilli());
    license.getModuleLicenses().values().forEach(
        l -> l.setAccountIdentifier(existingEntityOptional.get().getAccountIdentifier()));
    validateAccountLicense(license);
    accountLicenseCheckHandler.handle(license);
    AccountLicense savedEntity = accountLicenseRepository.save(license);

    return accountLicenseMapper.toDTO(savedEntity);
  }

  @Override
  public ModuleLicenseDTO startTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO) {
    Edition edition = Edition.ENTERPRISE;
    LicenseType licenseType = LicenseType.TRIAL;
    ModuleType moduleType = startTrialRequestDTO.getModuleType();
    ModuleLicenseDTO trialLicenseDTO =
        licenseInterface.generateTrialLicense(edition, accountIdentifier, licenseType, moduleType);
    ModuleLicense trialLicense = licenseObjectMapper.toEntity(trialLicenseDTO);
    trialLicense.setCreatedBy(EmbeddedUser.builder().email(getEmailFromPrincipal()).build());

    AccountLicense accountLicense = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (hasAccountLicense(accountLicense)) {
      ModuleLicense existingLicense = accountLicense.getModuleLicenses().get(moduleType);
      if (existingLicense != null) {
        String cause = format("Trial license for moduleType [%s] already started in account [%s]",
            startTrialRequestDTO.getModuleType(), accountIdentifier);
        sendFailedTelemetryEvents(accountIdentifier, startTrialRequestDTO.getModuleType(), licenseType, edition, cause);
        throw new DuplicateFieldException(cause);
      } else {
        // already started trial in other module
        accountLicense.getModuleLicenses().put(moduleType, trialLicense);
      }
    } else {
      // first time start trial in account, create account license along with trial module license
      accountLicense = AccountLicense.builder()
                           .accountIdentifier(accountIdentifier)
                           .moduleLicenses(ImmutableMap.of(moduleType, trialLicense))
                           .allInactive(false)
                           .build();
    }
    validateAccountLicense(accountLicense);
    AccountLicense savedEntity = accountLicenseRepository.save(accountLicense);
    sendSucceedTelemetryEvents(
        SUCCEED_START_TRIAL_OPERATION, savedEntity.getModuleLicenses().get(moduleType), accountIdentifier);

    log.info("Trial license for module [{}] is started in account [{}]", startTrialRequestDTO.getModuleType(),
        accountIdentifier);
    accountService.updateDefaultExperienceIfApplicable(accountIdentifier, DefaultExperience.NG);
    return licenseObjectMapper.toDTO(savedEntity.getModuleLicenses().get(moduleType));
  }

  @Override
  public ModuleLicenseDTO extendTrialLicense(String accountIdentifier, StartTrialRequestDTO startTrialRequestDTO) {
    Edition edition = Edition.ENTERPRISE;
    LicenseType licenseType = LicenseType.TRIAL;
    ModuleType moduleType = startTrialRequestDTO.getModuleType();
    AccountLicense accountLicense = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (isNotStartedTrial(accountLicense, moduleType)) {
      String cause = format("Trial license for moduleType [%s] hasn't started in account [%s]",
          startTrialRequestDTO.getModuleType(), accountIdentifier);
      sendFailedTelemetryEvents(accountIdentifier, startTrialRequestDTO.getModuleType(), licenseType, edition, cause);
      throw new InvalidRequestException(cause);
    }

    ModuleLicense moduleLicense = accountLicense.getModuleLicenses().get(moduleType);
    if (isNotEligibleToExtend(moduleLicense)) {
      String cause = format("Can not extend trial for account [%s]. Please contact sales.", accountIdentifier);
      sendFailedTelemetryEvents(accountIdentifier, startTrialRequestDTO.getModuleType(), licenseType, edition, cause);
      throw new InvalidRequestException(cause);
    }

    LicenseTransaction expiredTranscation = moduleLicense.getTransactions().get(0);
    LicenseTransaction extendTransaction = expiredTranscation.makeTemplateCopy();
    extendTransaction.setStartTime(Instant.now().toEpochMilli());
    extendTransaction.setExpiryTime(Instant.now().plus(TRIAL_DURATION, ChronoUnit.DAYS).toEpochMilli());
    extendTransaction.setStatus(LicenseStatus.ACTIVE);
    extendTransaction.setUuid(ObjectId.get().toHexString());

    moduleLicense.getTransactions().add(extendTransaction);
    accountLicenseCheckHandler.handle(accountLicense);
    AccountLicense savedEntity = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);

    sendSucceedTelemetryEvents(
        SUCCEED_EXTEND_TRIAL_OPERATION, savedEntity.getModuleLicenses().get(moduleType), accountIdentifier);
    log.info("Trial license for module [{}] is extended in account [{}]", startTrialRequestDTO.getModuleType(),
        accountIdentifier);
    return licenseObjectMapper.toDTO(savedEntity.getModuleLicenses().get(moduleType));
  }

  @Override
  public boolean shouldRemoveAccount(String accountIdentifier) {
    AccountLicense accountLicense = accountLicenseRepository.findByAccountIdentifier(accountIdentifier);
    if (accountLicense == null) {
      return true;
    }
    boolean allInactive = accountLicense.isAllInactive();
    long paidCount = accountLicense.getModuleLicenses()
                         .values()
                         .stream()
                         .filter(m -> LicenseType.PAID.equals(m.getLicenseType()))
                         .count();
    return allInactive && paidCount == 0;
  }

  @Override
  public void softDelete(String accountIdentifier) {
    // TODO implement soft delete account logic here
  }

  private boolean hasAccountLicense(AccountLicense accountLicense) {
    return accountLicense != null && accountLicense.getModuleLicenses() != null;
  }

  private boolean isNotStartedTrial(AccountLicense accountLicense, ModuleType moduleType) {
    return accountLicense == null || accountLicense.getModuleLicenses() == null
        || accountLicense.getModuleLicenses().get(moduleType) == null;
  }

  private boolean isNotEligibleToExtend(ModuleLicense moduleLicense) {
    Duration duration = Duration.ofMillis(Instant.now().toEpochMilli() - moduleLicense.getExpireTime());
    return moduleLicense.isActive() || duration.toDays() > 14 || LicenseType.PAID.equals(moduleLicense.getLicenseType())
        || Edition.FREE.equals(moduleLicense.getEdition()) || moduleLicense.getTransactions().size() != 1;
  }

  private void validateAccountLicense(AccountLicense accountLicense) {
    accountLicense.getModuleLicenses().values().forEach(m -> setIdForTransactionIfNull(m));
  }

  private void verifyTransactionsAlignment(AccountLicense accountLicense) {
    accountLicense.getModuleLicenses().values().forEach(m -> {
      LicenseType licenseType = m.getLicenseType();
      Edition edition = m.getEdition();
      long currentTime = Instant.now().toEpochMilli();
      for (LicenseTransaction transaction : m.getTransactions()) {
        if (!transaction.checkExpiry(currentTime)) {
          transaction.setStatus(LicenseStatus.ACTIVE);
        }

        if (transaction.isActive()) {
          if (edition != transaction.getEdition() || licenseType != transaction.getLicenseType()) {
            throw new InvalidRequestException(String.format(
                "AccountLicense has unmatched edition or licenseType between transactions and moduleLicense on %s, please check",
                m.getModuleType()));
          }
        }
      }
    });
  }

  private void setIdForTransactionIfNull(ModuleLicense moduleLicense) {
    List<LicenseTransaction> transactions = moduleLicense.getTransactions();
    if (transactions != null) {
      transactions.stream().filter(t -> t.getUuid() == null).forEach(t -> t.setUuid(ObjectId.get().toHexString()));
    }
  }

  private void sendSucceedTelemetryEvents(String eventName, ModuleLicense moduleLicense, String accountIdentifier) {
    String email = getEmailFromPrincipal();
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", email);
    properties.put("module", moduleLicense.getModuleType());
    properties.put("licenseType", moduleLicense.getLicenseType());
    properties.put("plan", moduleLicense.getEdition());
    properties.put("platform", "NG");
    properties.put("startTime", String.valueOf(moduleLicense.getTransactions().get(0).getStartTime()));
    properties.put("duration", TRIAL_DURATION);
    properties.put("licenseStatus", moduleLicense.getStatus());
    telemetryReporter.sendTrackEvent(eventName, properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build(), Category.SIGN_UP);

    HashMap<String, Object> groupProperties = new HashMap<>();
    String moduleType = moduleLicense.getModuleType().name();
    groupProperties.put(format("%s%s", moduleType, "LicenseEdition"), moduleLicense.getEdition());
    groupProperties.put(format("%s%s", moduleType, "LicenseType"), moduleLicense.getLicenseType());
    groupProperties.put(
        format("%s%s", moduleType, "LicenseStartTinme"), moduleLicense.getTransactions().get(0).getStartTime());
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
    properties.put("licenseType", licenseType);
    properties.put("licenseEdition", edition);
    telemetryReporter.sendTrackEvent(FAILED_OPERATION, properties, null, Category.SIGN_UP);
  }

  private String getEmailFromPrincipal() {
    Principal principal = SourcePrincipalContextBuilder.getSourcePrincipal();
    String email = "";
    if (principal instanceof UserPrincipal) {
      email = ((UserPrincipal) principal).getEmail();
    }
    return email;
  }
}
