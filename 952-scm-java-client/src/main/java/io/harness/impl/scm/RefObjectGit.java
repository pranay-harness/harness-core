package io.harness.impl.scm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefObjectGit {
  String sha;
  String type;
}
