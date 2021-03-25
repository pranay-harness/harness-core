package io.harness.serializer.kryo;

import io.harness.beans.CommitDetails;
import io.harness.beans.Repository;
import io.harness.beans.WebhookGitUser;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class ScmJavaClientKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CommitDetails.class, 955000);
    kryo.register(Repository.class, 955001);
    kryo.register(WebhookGitUser.class, 955002);
  }
}