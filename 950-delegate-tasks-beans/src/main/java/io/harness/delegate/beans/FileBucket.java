package io.harness.delegate.beans;

public enum FileBucket {
  LOB,
  ARTIFACTS,
  AUDITS,
  CONFIGS,
  LOGS,
  PLATFORMS,
  TERRAFORM_STATE,
  PROFILE_RESULTS,
  TERRAFORM_PLAN,
  EXPORT_EXECUTIONS;

  private int chunkSize;

  /**
   * Instantiates a new file bucket.
   *
   * @param chunkSize the chunk size
   */
  FileBucket(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  /**
   * Instantiates a new file bucket.
   */
  FileBucket() {
    this(1000 * 1000);
  }

  public String representationName() {
    return name().toLowerCase();
  }

  /**
   * Gets chunk size.
   *
   * @return the chunk size
   */
  public int getChunkSize() {
    return chunkSize;
  }
}
