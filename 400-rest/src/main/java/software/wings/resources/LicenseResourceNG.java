/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.license.CeLicenseInfoDTO;
import io.harness.ccm.license.CeLicenseType;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.Account;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api(value = "/ng/licenses", hidden = true)
@Path("/ng/licenses")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.GTM)
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
public class LicenseResourceNG {
  private final LicenseService licenseService;
  private final AccountService accountService;

  @POST
  @Path("ce/trial")
  public RestResponse<Boolean> startTrial(@NotNull CeLicenseInfoDTO dto) {
    Account account = accountService.get(dto.getAccountId());
    if (account == null || account.getCeLicenseInfo() != null) {
      throw new InvalidRequestException("Can't start CE Limited Trial license");
    }
    CeLicenseInfo ceLicenseInfo = convertDTO(dto);
    licenseService.updateCeLicense(dto.getAccountId(), ceLicenseInfo);
    return new RestResponse<>(true);
  }

  @PUT
  @Path("ce")
  public RestResponse<Boolean> updateCeLicense(@NotNull CeLicenseInfoDTO dto) {
    CeLicenseInfo ceLicenseInfo = convertDTO(dto);
    licenseService.updateCeLicense(dto.getAccountId(), ceLicenseInfo);
    return new RestResponse<>(true);
  }

  private CeLicenseInfo convertDTO(CeLicenseInfoDTO dto) {
    return CeLicenseInfo.builder().expiryTime(dto.getExpiryTime()).licenseType(CeLicenseType.LIMITED_TRIAL).build();
  }
}
