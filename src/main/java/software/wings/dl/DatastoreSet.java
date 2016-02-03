package software.wings.dl;

import org.mongodb.morphia.Datastore;

/**
 *  DatastoreSet is used to keep track of the connection pool to the primary and slave MongoDB hosts.
 *
 *
 * @author Rishi
 *
 */

public class DatastoreSet {
  private Datastore readWrite;
  private Datastore read;

  public Datastore getReadWrite() {
    return readWrite;
  }
  public void setReadWrite(Datastore readWrite) {
    this.readWrite = readWrite;
  }
  public Datastore getRead() {
    return read;
  }
  public void setRead(Datastore read) {
    this.read = read;
  }
}
