package io.harness.beans.plugin.compatible;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.yaml.extended.container.ContainerResource;

public interface PluginCompatibleStep extends CIStepInfo {
  // Common for all plugin compatible step types
  String getConnectorRef();
  String getImage();
  ContainerResource getResources();
  Integer getPort();
  void setPort(Integer port);
  String getCallbackId();
  void setCallbackId(String callbackId);
}
