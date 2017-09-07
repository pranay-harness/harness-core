package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class YamlSubList {
  @YamlSerialize public String name;
  @YamlSerialize public List<String> subList = new ArrayList<>();

  public YamlSubList() {}

  public YamlSubList(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getSubList() {
    return subList;
  }

  public void setSubList(List<String> subList) {
    this.subList = subList;
  }
}
