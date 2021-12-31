package io.harness.template.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.NGTemplateException;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class TemplateYamlConversionHandlerRegistry {
  private final Map<String, YamlConversionHandler> registry = new ConcurrentHashMap<>();

  public void register(String templateType, YamlConversionHandler yamlConversionHandler) {
    if (registry.containsKey(templateType)) {
      throw new NGTemplateException("YamlConversionHandler already Registered with type: " + templateType);
    }
    registry.put(templateType, yamlConversionHandler);
  }

  public YamlConversionHandler obtain(String templateType) {
    if (registry.containsKey(templateType)) {
      return registry.get(templateType);
    }
    throw new NGTemplateException("No yamlConversionHandler registered for type: " + templateType);
  }
}
