package io.harness.pms.sample.cd.beans;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class Execution {
  String uuid;
  List<JsonNode> steps;
}
