/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.impl.WebhookParserSCMServiceImpl;
import io.harness.impl.scm.SCMServiceGitClientImpl;
import io.harness.impl.scm.ScmServiceClientImpl;
import io.harness.service.ScmClient;
import io.harness.service.ScmServiceClient;
import io.harness.service.WebhookParserSCMService;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class SCMJavaClientModule extends AbstractModule {
  private static final AtomicReference<SCMJavaClientModule> instanceRef = new AtomicReference();

  public SCMJavaClientModule() {}

  @Override
  protected void configure() {
    bind(WebhookParserSCMService.class).to(WebhookParserSCMServiceImpl.class);
    bind(ScmClient.class).to(SCMServiceGitClientImpl.class);
    bind(ScmServiceClient.class).to(ScmServiceClientImpl.class);
  }

  public static SCMJavaClientModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new SCMJavaClientModule());
    }
    return instanceRef.get();
  }
}
