package io.harness.walktree.visitor;

/**
 * Any class which is using impl such as ValidationVisitor or InputSetMergeVisitor should impl this,
 * if they want to store extra information for each element node.
 */
public interface WithMetadata {
  String METADATA = "metadata";
  /**
   * This field is needed to set extra info for that visited element.
   * @return String.
   */
  String getMetadata();

  /**
   * Setter method for metadata
   */
  void setMetadata(String metadata);
}
