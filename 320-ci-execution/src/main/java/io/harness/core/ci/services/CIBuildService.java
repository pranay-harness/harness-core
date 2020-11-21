package io.harness.core.ci.services;

import io.harness.ci.beans.entities.CIBuild;
import io.harness.validation.Create;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

public interface CIBuildService {
  @ValidationGroups(Create.class) CIBuild save(CIBuild ciBuild);
}
