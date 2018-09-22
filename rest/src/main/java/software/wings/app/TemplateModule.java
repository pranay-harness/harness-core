package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import software.wings.beans.template.TemplateType;
import software.wings.service.impl.template.AbstractTemplateProcessor;
import software.wings.service.impl.template.HttpTemplateProcessor;
import software.wings.service.impl.template.SshCommandTemplateProcessor;
import software.wings.service.impl.template.TemplateFolderServiceImpl;
import software.wings.service.impl.template.TemplateGalleryServiceImpl;
import software.wings.service.impl.template.TemplateServiceImpl;
import software.wings.service.impl.template.TemplateVersionServiceImpl;
import software.wings.service.intfc.template.TemplateFolderService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

public class TemplateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(TemplateGalleryService.class).to(TemplateGalleryServiceImpl.class);
    bind(TemplateService.class).to(TemplateServiceImpl.class);
    bind(TemplateFolderService.class).to(TemplateFolderServiceImpl.class);
    bind(TemplateVersionService.class).to(TemplateVersionServiceImpl.class);

    MapBinder<String, AbstractTemplateProcessor> templateServiceBinder =
        MapBinder.newMapBinder(binder(), String.class, AbstractTemplateProcessor.class);

    templateServiceBinder.addBinding(TemplateType.SSH.name()).to(SshCommandTemplateProcessor.class);
    templateServiceBinder.addBinding(TemplateType.HTTP.name()).to(HttpTemplateProcessor.class);
  }
}
