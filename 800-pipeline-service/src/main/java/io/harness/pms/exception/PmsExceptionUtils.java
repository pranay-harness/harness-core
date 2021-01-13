package io.harness.pms.exception;

import io.harness.pms.contracts.plan.YamlFieldBlob;
import io.harness.pms.yaml.YamlField;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PmsExceptionUtils {
  public String getUnresolvedDependencyErrorMessage(Collection<YamlFieldBlob> yamlFieldBlobs) throws IOException {
    return String.format("Following Nodes could not be parsed: %s.",
        getYamlNodeErrorInfo(yamlFieldBlobs).stream().map(YamlNodeErrorInfo::toJson).collect(Collectors.joining(",")));
  }

  private List<YamlNodeErrorInfo> getYamlNodeErrorInfo(Collection<YamlFieldBlob> yamlFieldBlobs) throws IOException {
    List<YamlNodeErrorInfo> yamlNodeErrorInfos = new ArrayList<>();
    for (YamlFieldBlob yamlFieldBlob : yamlFieldBlobs) {
      YamlField yamlField = YamlField.fromFieldBlob(yamlFieldBlob);
      yamlNodeErrorInfos.add(YamlNodeErrorInfo.builder()
                                 .identifier(yamlField.getNode().getIdentifier())
                                 .name(yamlField.getName())
                                 .type(yamlField.getNode().getType())
                                 .build());
    }
    return yamlNodeErrorInfos;
  }
}
