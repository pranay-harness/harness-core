package io.harness.gitsync.persistance;

public interface YamlEntityConverter<Y, B> {
  B toBean(Y yaml);
  Y toYaml(B bean);
}
