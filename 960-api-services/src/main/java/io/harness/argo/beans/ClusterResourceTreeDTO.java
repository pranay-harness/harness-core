package io.harness.argo.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClusterResourceTreeDTO {
    Node root;
    public static class Node {
       String name;
       String kind;
       String namespace;
       String type;
       List<Node> children;
    }
}
