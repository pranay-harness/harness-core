/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.app;

import software.wings.beans.template.TemplateType;
import software.wings.service.impl.template.AbstractTemplateProcessor;
import software.wings.service.impl.template.ArtifactSourceTemplateProcessor;
import software.wings.service.impl.template.CustomDeploymentTypeProcessor;
import software.wings.service.impl.template.HttpTemplateProcessor;
import software.wings.service.impl.template.ImportedTemplateServiceImpl;
import software.wings.service.impl.template.PcfCommandTemplateProcessor;
import software.wings.service.impl.template.ShellScriptTemplateProcessor;
import software.wings.service.impl.template.SshCommandTemplateProcessor;
import software.wings.service.impl.template.TemplateFolderServiceImpl;
import software.wings.service.impl.template.TemplateGalleryServiceImpl;
import software.wings.service.impl.template.TemplateServiceImpl;
import software.wings.service.impl.template.TemplateVersionServiceImpl;
import software.wings.service.intfc.template.ImportedTemplateService;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class TemplateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(TemplateGalleryService.class).to(TemplateGalleryServiceImpl.class);
    bind(TemplateService.class).to(TemplateServiceImpl.class);
    bind(TemplateFolderService.class).to(TemplateFolderServiceImpl.class);
    bind(TemplateVersionService.class).to(TemplateVersionServiceImpl.class);
    bind(ImportedTemplateService.class).to(ImportedTemplateServiceImpl.class);
    MapBinder<String, AbstractTemplateProcessor> templateServiceBinder =
        MapBinder.newMapBinder(binder(), String.class, AbstractTemplateProcessor.class);

    templateServiceBinder.addBinding(TemplateType.SSH.name()).to(SshCommandTemplateProcessor.class);
    templateServiceBinder.addBinding(TemplateType.HTTP.name()).to(HttpTemplateProcessor.class);
    templateServiceBinder.addBinding(TemplateType.SHELL_SCRIPT.name()).to(ShellScriptTemplateProcessor.class);
    templateServiceBinder.addBinding(TemplateType.ARTIFACT_SOURCE.name()).to(ArtifactSourceTemplateProcessor.class);
    templateServiceBinder.addBinding(TemplateType.PCF_PLUGIN.name()).to(PcfCommandTemplateProcessor.class);
    templateServiceBinder.addBinding(TemplateType.CUSTOM_DEPLOYMENT_TYPE.name())
        .to(CustomDeploymentTypeProcessor.class);
  }
}
