package software.wings.common;

import static io.harness.idempotence.IdempotentRegistry.State.DONE;
import static io.harness.idempotence.IdempotentRegistry.State.NEW;
import static java.util.Arrays.asList;
import static software.wings.beans.Idempotent.SUCCEEDED;
import static software.wings.beans.Idempotent.TENTATIVE;

import com.google.inject.Inject;

import com.mongodb.MongoCommandException;
import com.mongodb.WriteConcern;
import io.harness.exception.UnableToRegisterIdempotentOperationException;
import io.harness.idempotence.IdempotentId;
import io.harness.idempotence.IdempotentRegistry;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Idempotent;
import software.wings.dl.WingsPersistence;

public class MongoIdempotentRegistry<T> implements IdempotentRegistry<T> {
  private static final Logger logger = LoggerFactory.getLogger(MongoIdempotentRegistry.class);

  public static final FindAndModifyOptions registerOptions =
      new FindAndModifyOptions().returnNew(false).upsert(true).writeConcern(new WriteConcern("majority"));
  public static final FindAndModifyOptions unregisterOptions =
      new FindAndModifyOptions().remove(true).writeConcern(new WriteConcern("majority"));

  @Inject private WingsPersistence wingsPersistence;

  public UpdateOperations<Idempotent> registerUpdateOperation() {
    return wingsPersistence.createUpdateOperations(Idempotent.class).set("state", TENTATIVE);
  }

  public UpdateOperations<Idempotent> unregisterUpdateOperation() {
    return wingsPersistence.createUpdateOperations(Idempotent.class);
  }

  public Query<Idempotent> query(IdempotentId id) {
    return wingsPersistence.createQuery(Idempotent.class)
        .filter(Idempotent.ID_KEY, id.getValue())
        .filter("state !=", SUCCEEDED);
  }

  @Override
  public Response register(IdempotentId id) throws UnableToRegisterIdempotentOperationException {
    try {
      // Insert new record in the idempotent collection with a tentative state
      final Idempotent idempotent =
          wingsPersistence.findAndModify(query(id), registerUpdateOperation(), registerOptions);

      // If there was no record from before, we are the first to handle this operation
      if (idempotent == null) {
        return Response.builder().state(NEW).build();
      }
    } catch (MongoCommandException exception) {
      // If we failed with duplicate key - there is already successful operation in the db
      if (exception.getMessage().contains("E11000 ")) {
        Idempotent idempotent = wingsPersistence.get(Idempotent.class, id.getValue());
        return Response.builder().state(DONE).result((T) idempotent.getResult().get(0)).build();
      }
      throw new UnableToRegisterIdempotentOperationException(exception);
    } catch (RuntimeException exception) {
      throw new UnableToRegisterIdempotentOperationException(exception);
    }

    // If there was already record, but it was not successful, it is still tentative
    return Response.builder().state(NEW).build();
  }

  @Override
  public void unregister(IdempotentId id) {
    // Delete the operation record
    wingsPersistence.findAndModify(query(id), unregisterUpdateOperation(), unregisterOptions);
  }

  @Override
  public void finish(IdempotentId id, T data) {
    Idempotent newIdempotent = new Idempotent();
    newIdempotent.setUuid(id.getValue());
    newIdempotent.setState(SUCCEEDED);
    newIdempotent.setResult(asList((Object) data));
    wingsPersistence.save(newIdempotent);
  }
}
