package io.harness.pms.inputset.fqn;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FQN {
  private List<FQNNode> fqnList;

  public static FQN duplicateAndAddNode(FQN base, FQNNode newNode) {
    List<FQNNode> newList = new ArrayList<>(base.getFqnList());
    newList.add(newNode);
    return FQN.builder().fqnList(newList).build();
  }

  public String display() {
    StringBuilder res = new StringBuilder();
    for (FQNNode node : fqnList) {
      if (node.getNodeType() == FQNNode.NodeType.KEY) {
        res.append(node.getKey()).append(".");
      } else if (node.getNodeType() == FQNNode.NodeType.KEY_WITH_UUID) {
        res.append(node.getKey())
            .append("[")
            .append(node.getUuidKey())
            .append(":")
            .append(node.getUuidValue())
            .append("].");
      } else if (node.getNodeType() == FQNNode.NodeType.PARALLEL) {
        res.append("PARALLEL.");
      } else if (node.getNodeType() == FQNNode.NodeType.UUID) {
        res.append("[").append(node.getUuidKey()).append(":").append(node.getUuidValue()).append("].");
      }
    }
    return res.toString();
  }

  public boolean contains(FQN baseFQN) {
    String subString = baseFQN.display();
    String string = this.display();
    return string.startsWith(subString);
  }

  public boolean isIdentifierOrVariableName() {
    boolean isVariableName = isVariableName();
    FQNNode lastNode = fqnList.get(fqnList.size() - 1);
    return isVariableName || lastNode.getKey().equals("identifier");
  }

  public boolean isVariableName() {
    if (fqnList.size() < 2) {
      return false;
    }
    FQNNode lastNode = fqnList.get(fqnList.size() - 1);
    FQNNode secondLastNode = fqnList.get(fqnList.size() - 2);
    return secondLastNode.getNodeType().equals(FQNNode.NodeType.UUID) && lastNode.getKey().equals("name");
  }

  public String getFieldName() {
    return fqnList.get(fqnList.size() - 1).getKey();
  }
}
