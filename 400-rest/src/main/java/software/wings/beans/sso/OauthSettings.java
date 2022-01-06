/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.account.OauthProviderType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("OAUTH")
public class OauthSettings extends SSOSettings {
  @NotBlank private String accountId;
  private String filter;
  private Set<OauthProviderType> allowedProviders;

  @JsonCreator
  @Builder
  public OauthSettings(@JsonProperty("accountid") String accountId, @JsonProperty("displayName") String displayName,
      @JsonProperty("filter") String filter,
      @JsonProperty("allowedProviders") Set<OauthProviderType> allowedProviders) {
    super(SSOType.OAUTH, allowedProviders.stream().map(OauthProviderType::name).collect(Collectors.joining(",")), "");
    this.accountId = accountId;
    this.filter = filter;
    if (isNotEmpty(displayName)) {
      this.displayName = displayName;
    }
    this.allowedProviders = allowedProviders;
  }

  @Override
  public SSOType getType() {
    return SSOType.OAUTH;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }
}
