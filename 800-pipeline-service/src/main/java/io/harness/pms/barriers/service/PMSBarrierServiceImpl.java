package io.harness.pms.barriers.service;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.barriers.beans.BarrierSetupInfo;
import io.harness.pms.barriers.visitor.BarrierVisitor;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class PMSBarrierServiceImpl implements PMSBarrierService {
  private final Injector injector;

  @Override
  public List<BarrierSetupInfo> getBarrierSetupInfoList(String yaml) {
    try {
      YamlNode yamlNode = YamlUtils.extractPipelineField(yaml).getNode();
      BarrierVisitor barrierVisitor = new BarrierVisitor(injector);
      barrierVisitor.walkElementTree(yamlNode);
      return new ArrayList<>(barrierVisitor.getBarrierIdentifierMap().values());
    } catch (IOException e) {
      log.error("Error while extracting yaml");
      throw new InvalidRequestException("Error while extracting yaml", e);
    } catch (InvalidRequestException e) {
      log.error("Error while processing yaml");
      throw e;
    }
  }
}
