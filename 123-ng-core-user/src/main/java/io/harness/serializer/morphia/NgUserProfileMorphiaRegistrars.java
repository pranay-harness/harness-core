/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.userprofile.entities.AwsCodeCommitSCM;
import io.harness.ng.userprofile.entities.AzureDevOpsSCM;
import io.harness.ng.userprofile.entities.BitbucketSCM;
import io.harness.ng.userprofile.entities.GithubSCM;
import io.harness.ng.userprofile.entities.GitlabSCM;
import io.harness.ng.userprofile.entities.SourceCodeManager;

import java.util.Set;

@OwnedBy(PL)
public class NgUserProfileMorphiaRegistrars implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(SourceCodeManager.class);
    set.add(BitbucketSCM.class);
    set.add(GithubSCM.class);
    set.add(GitlabSCM.class);
    set.add(AwsCodeCommitSCM.class);
    set.add(AzureDevOpsSCM.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
