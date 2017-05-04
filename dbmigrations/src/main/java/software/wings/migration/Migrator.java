package software.wings.migration;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import org.mongeez.Mongeez;
import org.mongeez.MongoAuth;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by peeyushaggarwal on 1/3/17.
 */
public class Migrator {
  public static void main(String... args) {
    Mongeez mongeez = new Mongeez();
    mongeez.setFile(new ClassPathResource("/mongeez.xml"));
    MongoClientURI clientUri = new MongoClientURI(System.getProperty("mongoUri", "mongodb://localhost:27017/wings"));
    MongoClient mongoClient = new MongoClient(clientUri);
    mongeez.setMongo(mongoClient);
    MongoCredential mc = clientUri.getCredentials();
    if (mc != null) {
      mongeez.setAuth(new MongoAuth(mc.getUserName(), new String(mc.getPassword()), mc.getSource()));
    }
    mongeez.setDbName(clientUri.getDatabase());
    mongeez.process();
    mongoClient.close();
  }
}
