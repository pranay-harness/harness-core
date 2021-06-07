package io.harness.argo.beans;

import io.harness.data.structure.EmptyPredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClusterResourceTreeDTO {
  Node root;

  @Data
  @Builder
  public static class Node {
    String name;
    String kind;
    String namespace;
    String type;
    List<Node> children;
  }

  public ClusterResourceTreeDTO(ClusterResourceTree clusterResourceTree, String appName) {
    //    ClusterResourceTreeDTO tree = ClusterResourceTreeDTO.builder().build();
    this.root = Node.builder().name(appName).children(buildTree(clusterResourceTree.getNodes(), "")).build();
  }

  private List<Node> buildTree(List<ClusterResourceTree.Node> nodes, String parentRef) {
    final List<Node> tree = new ArrayList<>();
    final List<ClusterResourceTree.Node> parents =
        nodes.stream().filter(n -> n.getParentRef().equals(parentRef)).collect(Collectors.toList());
    if (EmptyPredicate.isEmpty(parents))
      return tree;
    tree.addAll(parents.stream()
                    .map(p -> Node.builder().name(p.getName()).children(buildTree(nodes, p.getUid())).build())
                    .collect(Collectors.toList()));
    return tree;
  }
}
