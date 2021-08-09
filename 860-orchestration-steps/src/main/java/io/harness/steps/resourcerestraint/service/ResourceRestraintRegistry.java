package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.ConstraintRegistry;

@OwnedBy(PIPELINE)
public interface ResourceRestraintRegistry extends ConstraintRegistry {}
