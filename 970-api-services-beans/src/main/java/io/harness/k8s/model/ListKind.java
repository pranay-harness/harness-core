package io.harness.k8s.model;

public enum ListKind {
  List(null),
  NamespaceList(Kind.Namespace),
  ServiceList(Kind.Service),
  DeploymentList(Kind.Deployment),
  RoleBindingList(Kind.RoleBinding),
  ClusterRoleBindingList(Kind.ClusterRoleBinding);

  private final Kind itemKind;

  ListKind(Kind itemKind) {
    this.itemKind = itemKind;
  }

  public Kind getItemKind() {
    return itemKind;
  }
}
