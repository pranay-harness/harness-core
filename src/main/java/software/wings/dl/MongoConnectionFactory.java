package software.wings.dl;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.MongoClient;

import io.dropwizard.setup.Environment;
import software.wings.beans.Application;
import software.wings.beans.AuditHeader;
import software.wings.beans.AuditPayload;

/**
 *  MongoConnectionFactory is used to establish the MongoDB connection pool.
 *
 *
 * @author Rishi
 *
 */
public class MongoConnectionFactory {
  @NotEmpty private String host = "localhost";

  @Min(1) @Max(65535) @JsonProperty private int port = 27017;

  @JsonProperty @NotEmpty private String db = "meta";

  @JsonProperty
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @JsonProperty
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @JsonProperty
  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public MongoClient getMongoClient() {
    if (mongoClientInstance == null) {
      synchronized (this) {
        if (mongoClientInstance == null) {
          mongoClientInstance = new MongoClient(getHost(), getPort());
        }
      }
    }
    return mongoClientInstance;
  }

  public Datastore getDatastore() {
    Morphia m = new Morphia();
    Datastore ds = m.createDatastore(getMongoClient(), getDb());

    // TODO - Need to automate package scanning
    m.map(Application.class);
    m.map(AuditHeader.class);
    m.map(AuditPayload.class);

    ds.ensureIndexes();
    return ds;
  }

  private MongoClient mongoClientInstance;
  private Datastore datastore;

  public void initialize(Environment environment) {
    DatastoreSet datastoreSet = new DatastoreSet();
  }
}
