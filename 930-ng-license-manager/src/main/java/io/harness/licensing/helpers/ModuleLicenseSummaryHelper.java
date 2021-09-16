package io.harness.licensing.helpers;

import io.harness.ModuleType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.summary.CDLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CELicenseSummaryDTO;
import io.harness.licensing.beans.summary.CFLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CILicenseSummaryDTO;
import io.harness.licensing.beans.summary.CVLicenseSummaryDTO;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.utils.ModuleLicenseUtils;

import com.google.inject.Singleton;
import java.time.Instant;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@Singleton
public class ModuleLicenseSummaryHelper {
  public static LicensesWithSummaryDTO generateSummary(
      ModuleType moduleType, List<ModuleLicenseDTO> moduleLicenseDTOs) {
    long currentTime = Instant.now().toEpochMilli();

    SummaryHandler summaryHandler;
    LicensesWithSummaryDTO licensesWithSummaryDTO;
    switch (moduleType) {
      case CI:
        licensesWithSummaryDTO = CILicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CIModuleLicenseDTO temp = (CIModuleLicenseDTO) moduleLicenseDTO;
          CILicenseSummaryDTO ciLicenseSummaryDTO = (CILicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfCommitters() != null) {
              ciLicenseSummaryDTO.setTotalDevelopers(ModuleLicenseUtils.computeAdd(
                  ciLicenseSummaryDTO.getTotalDevelopers(), temp.getNumberOfCommitters()));
            }
          }
        };
        break;
      case CD:
        licensesWithSummaryDTO = CDLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CDModuleLicenseDTO temp = (CDModuleLicenseDTO) moduleLicenseDTO;
          CDLicenseSummaryDTO cdLicenseSummaryDTO = (CDLicenseSummaryDTO) summaryDTO;

          if (current < temp.getExpiryTime()) {
            if (temp.getWorkloads() != null) {
              cdLicenseSummaryDTO.setTotalWorkload(
                  ModuleLicenseUtils.computeAdd(cdLicenseSummaryDTO.getTotalWorkload(), temp.getWorkloads()));
            }
            if (temp.getServiceInstances() != null) {
              cdLicenseSummaryDTO.setTotalServiceInstances(ModuleLicenseUtils.computeAdd(
                  cdLicenseSummaryDTO.getTotalServiceInstances(), temp.getServiceInstances()));
            }
          }
        };
        break;
      case CV:
        licensesWithSummaryDTO = CVLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {};
        break;
      case CF:
        licensesWithSummaryDTO = CFLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CFModuleLicenseDTO temp = (CFModuleLicenseDTO) moduleLicenseDTO;
          CFLicenseSummaryDTO cfLicenseSummaryDTO = (CFLicenseSummaryDTO) summaryDTO;

          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfClientMAUs() != null) {
              cfLicenseSummaryDTO.setTotalClientMAUs(ModuleLicenseUtils.computeAdd(
                  cfLicenseSummaryDTO.getTotalClientMAUs(), temp.getNumberOfClientMAUs()));
            }
            if (temp.getNumberOfUsers() != null) {
              cfLicenseSummaryDTO.setTotalFeatureFlagUnits(ModuleLicenseUtils.computeAdd(
                  cfLicenseSummaryDTO.getTotalFeatureFlagUnits(), temp.getNumberOfUsers()));
            }
          }
        };
        break;
      case CE:
        licensesWithSummaryDTO = CELicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CEModuleLicenseDTO temp = (CEModuleLicenseDTO) moduleLicenseDTO;
          CELicenseSummaryDTO ceLicenseSummaryDTO = (CELicenseSummaryDTO) summaryDTO;

          if (current < temp.getExpiryTime()) {
            if (temp.getSpendLimit() != null) {
              ceLicenseSummaryDTO.setTotalSpendLimit(
                  ModuleLicenseUtils.computeAdd(ceLicenseSummaryDTO.getTotalSpendLimit(), temp.getSpendLimit()));
            }
          }
        };
        break;
      default:
        throw new UnsupportedOperationException("Unsupported module type");
    }

    moduleLicenseDTOs.forEach(l -> {
      // calculate summary detail info via each moduleLicenseDTO
      summaryHandler.calculateModuleSummary(l, licensesWithSummaryDTO, currentTime);

      // Use the last expiring license info as the summary general info
      if (l.getExpiryTime() > licensesWithSummaryDTO.getMaxExpiryTime()) {
        licensesWithSummaryDTO.setMaxExpiryTime(l.getExpiryTime());
        licensesWithSummaryDTO.setEdition(l.getEdition());
        licensesWithSummaryDTO.setLicenseType(l.getLicenseType());
      }
    });
    licensesWithSummaryDTO.setModuleType(moduleType);
    return licensesWithSummaryDTO;
  }

  private interface SummaryHandler {
    void calculateModuleSummary(
        ModuleLicenseDTO moduleLicenseDTO, LicensesWithSummaryDTO licensesWithSummaryDTO, long currentTime);
  }
}
