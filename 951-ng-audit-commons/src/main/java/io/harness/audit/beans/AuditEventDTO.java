/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.request.HttpRequestInfo;
import io.harness.request.RequestMetadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AuditEventDTO {
  String auditId;
  @NotNull @NotBlank String insertId;
  @Valid @NotNull ResourceScopeDTO resourceScope;

  @Valid HttpRequestInfo httpRequestInfo;
  @Valid RequestMetadata requestMetadata;

  @NotNull Long timestamp;

  @NotNull @Valid AuthenticationInfoDTO authenticationInfo;

  @NotNull ModuleType module;
  @Valid Environment environment;

  @NotNull @Valid ResourceDTO resource;

  @ApiModelProperty(hidden = true) @Valid YamlDiffRecordDTO yamlDiffRecord;

  @NotNull Action action;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
  @Valid
  AuditEventData auditEventData;

  @ApiModelProperty(hidden = true) Map<String, String> internalInfo;
}
