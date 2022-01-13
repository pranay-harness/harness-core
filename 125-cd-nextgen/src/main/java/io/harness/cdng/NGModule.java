/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.NGBeanModule;
import io.harness.NGPipelineCommonsModule;
import io.harness.WalkTreeModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceService;
import io.harness.cdng.artifact.resources.docker.service.DockerResourceServiceImpl;
import io.harness.cdng.artifact.resources.ecr.service.EcrResourceService;
import io.harness.cdng.artifact.resources.ecr.service.EcrResourceServiceImpl;
import io.harness.cdng.artifact.resources.gcr.service.GcrResourceService;
import io.harness.cdng.artifact.resources.gcr.service.GcrResourceServiceImpl;
import io.harness.cdng.artifact.service.ArtifactSourceService;
import io.harness.cdng.artifact.service.impl.ArtifactSourceServiceImpl;
import io.harness.cdng.buckets.resources.s3.S3ResourceService;
import io.harness.cdng.buckets.resources.s3.S3ResourceServiceImpl;
import io.harness.cdng.buckets.resources.service.GcsResourceService;
import io.harness.cdng.buckets.resources.service.GcsResourceServiceImpl;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.instance.info.InstanceInfoServiceImpl;
import io.harness.cdng.jira.resources.service.JiraResourceService;
import io.harness.cdng.jira.resources.service.JiraResourceServiceImpl;
import io.harness.cdng.k8s.resources.gcp.service.GcpResourceService;
import io.harness.cdng.k8s.resources.gcp.service.impl.GcpResourceServiceImpl;
import io.harness.cdng.servicenow.resources.service.ServiceNowResourceService;
import io.harness.cdng.servicenow.resources.service.ServiceNowResourceServiceImpl;
import io.harness.cdng.usage.impl.CDLicenseUsageImpl;
import io.harness.cdng.yaml.CdYamlSchemaService;
import io.harness.cdng.yaml.CdYamlSchemaServiceImpl;
import io.harness.licensing.usage.interfaces.LicenseUsageInterface;
import io.harness.ng.core.NGCoreModule;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ng.core.service.services.impl.ServiceEntityServiceImpl;
import io.harness.service.instance.InstanceService;
import io.harness.service.instance.InstanceServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public class NGModule extends AbstractModule {
  private static final AtomicReference<NGModule> instanceRef = new AtomicReference<>();

  public static NGModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    install(NGCoreModule.getInstance());
    install(WalkTreeModule.getInstance());
    install(NGPipelineCommonsModule.getInstance());
    install(NGBeanModule.getInstance());

    bind(ArtifactSourceService.class).to(ArtifactSourceServiceImpl.class);
    bind(DockerResourceService.class).to(DockerResourceServiceImpl.class);
    bind(GcrResourceService.class).to(GcrResourceServiceImpl.class);
    bind(EcrResourceService.class).to(EcrResourceServiceImpl.class);
    bind(JiraResourceService.class).to(JiraResourceServiceImpl.class);
    bind(CdYamlSchemaService.class).to(CdYamlSchemaServiceImpl.class);
    bind(GcpResourceService.class).to(GcpResourceServiceImpl.class);
    bind(S3ResourceService.class).to(S3ResourceServiceImpl.class);
    bind(GcsResourceService.class).to(GcsResourceServiceImpl.class);
    bind(InstanceInfoService.class).to(InstanceInfoServiceImpl.class);
    bind(LicenseUsageInterface.class).to(CDLicenseUsageImpl.class);
    bind(InstanceService.class).to(InstanceServiceImpl.class);
    bind(ServiceEntityService.class).to(ServiceEntityServiceImpl.class);
    bind(ServiceNowResourceService.class).to(ServiceNowResourceServiceImpl.class);
  }
}
