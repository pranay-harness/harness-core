package io.harness.testlib.module;

import static io.harness.testlib.module.FakeMongoCreator.takeFakeMongo;
import static io.harness.testlib.module.RealMongoCreator.takeRealMongo;

import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.mongodb.MongoClient;
import io.harness.factory.ClosingFactory;
import io.harness.govern.ProviderModule;
import io.harness.testlib.RealMongo;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Random;

public interface MongoRuleMixin {
  enum MongoType { REAL, FAKE }

  default Module mongoTypeModule(List<Annotation> annotations) {
    return new ProviderModule() {
      @Provides
      @Singleton
      MongoType provideMongoType() {
        return annotations.stream().anyMatch(RealMongo.class ::isInstance) ? MongoType.REAL : MongoType.FAKE;
      }
    };
  }

  Random random = new Random();

  default String databaseName() {
    return System.getProperty(
        "dbName", String.format("unit_tests_%d_%d", 1000 + random.nextInt(8999), System.currentTimeMillis() / 1000));
  }

  default MongoClient fakeMongoClient(ClosingFactory closingFactory) {
    FakeMongoCreator.FakeMongo fakeMongo = takeFakeMongo();
    closingFactory.addServer(fakeMongo);
    return fakeMongo.getMongoClient();
  }

  default MongoClient realMongoClient(ClosingFactory closingFactory, String databaseName) {
    RealMongoCreator.RealMongo realMongo = takeRealMongo(databaseName);
    closingFactory.addServer(realMongo);
    return realMongo.getMongoClient();
  }
}
