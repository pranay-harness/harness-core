package io.harness.gitsync.scm.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class SCMNoOpResponse extends ScmPushResponse {}
