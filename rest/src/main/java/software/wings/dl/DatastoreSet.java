package software.wings.dl;

import org.mongodb.morphia.Datastore;

/**
 * DatastoreSet is used to keep track of the connection pool to the primary and slave MongoDB hosts.
 *
 * @author Rishi
 */
public class DatastoreSet {
  private Datastore readWrite;
  private Datastore read;

  /**
   * Gets read write.
   *
   * @return the read write
   */
  public Datastore getReadWrite() {
    return readWrite;
  }

  /**
   * Sets read write.
   *
   * @param readWrite the read write
   */
  public void setReadWrite(Datastore readWrite) {
    this.readWrite = readWrite;
  }

  /**
   * Gets read.
   *
   * @return the read
   */
  public Datastore getRead() {
    return read;
  }

  /**
   * Sets read.
   *
   * @param read the read
   */
  public void setRead(Datastore read) {
    this.read = read;
  }
}
