package io.harness.git.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP) public enum GitRepositoryType { YAML, TERRAFORM, AWS_SAM, TRIGGER, HELM, TERRAGRUNT }
