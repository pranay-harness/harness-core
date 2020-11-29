package io.harness.pms.sample.cd.beans;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Execution {
  String uuid;
  List<JsonNode> steps;
}
