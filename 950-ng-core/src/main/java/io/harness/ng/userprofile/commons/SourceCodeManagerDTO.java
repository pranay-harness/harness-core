/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.userprofile.commons;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.SourceCodeManagerAuthentication;
import io.harness.mongo.index.FdIndex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Data
@SuperBuilder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "SourceCodeManager", description = "This contains details of Source Code Manager")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class SourceCodeManagerDTO {
  @Schema(description = "Source Code Manager Identifier") String id;
  @Schema(description = "Id of the User") String userIdentifier;
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @FdIndex String accountIdentifier;
  @Schema(description = "Name of Source Code Manager") @NotNull String name;
  @Schema(description = "Time at which this Source Code Manager was created") Long createdAt;
  @Schema(description = "Time at which this Source Code Manager was last Updated") Long lastModifiedAt;
  @Schema(description = "Type of SCM") public abstract SCMType getType();
  @Schema(description = "Authentication Details of Source Code Manager")
  public abstract SourceCodeManagerAuthentication getAuthentication();
}
