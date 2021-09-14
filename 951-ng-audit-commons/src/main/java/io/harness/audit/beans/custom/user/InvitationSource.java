/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.audit.beans.custom.user;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.audit.beans.custom.user.InvitationSource.TYPE_NAME;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(TYPE_NAME)
@TypeAlias("InvitationSource")
public class InvitationSource extends Source {
  public static final String TYPE_NAME = "Invitation";

  @Builder
  public InvitationSource() {
    this.type = TYPE_NAME;
  }
}
