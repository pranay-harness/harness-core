package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.service.YamlChangeSetHandler;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(DX)
public class YamlChangeSetEventHandlerFactory {
  public YamlChangeSetHandler getChangeSetHandler(YamlChangeSetDTO yamlChangeSetDTO) {
    // define switch cases for handler
    //        switch (yamlChangeSetDTO.getEventType()){
    //            case

    //        }
    return null;
  }
}
