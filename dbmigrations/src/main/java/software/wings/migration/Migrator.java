package software.wings.migration;

import com.mongodb.MongoClient;
import org.mongeez.Mongeez;
import org.springframework.core.io.ClassPathResource;

/**
 * Created by peeyushaggarwal on 1/3/17.
 */
public class Migrator {
  public static void main(String... args) {
    Mongeez mongeez = new Mongeez();
    mongeez.setFile(new ClassPathResource("/mongeez.xml"));
    MongoClient mongoClient = new MongoClient(
        System.getProperty("mongoHost", "localhost"), Integer.parseInt(System.getProperty("mongoPort", "27017")));
    mongeez.setMongo(mongoClient);
    mongeez.setDbName("wings");
    mongeez.process();
    mongoClient.close();
  }
}
