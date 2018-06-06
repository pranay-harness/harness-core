package software.wings.common;

import static com.mongodb.ErrorCategory.DUPLICATE_KEY;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.DONE;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.NEW;
import static io.harness.distribution.idempotence.IdempotentRegistry.State.RUNNING;
import static java.util.Arrays.asList;
import static software.wings.beans.Idempotent.SUCCEEDED;
import static software.wings.beans.Idempotent.TENTATIVE;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoCommandException;
import com.mongodb.WriteConcern;
import io.harness.distribution.idempotence.IdempotentId;
import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.IdempotentRegistry;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Idempotent;
import software.wings.dl.WingsPersistence;

import java.time.Duration;

@Singleton
public class MongoIdempotentRegistry<T> implements IdempotentRegistry<T> {
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

  @Override
  public IdempotentLock create(IdempotentId id) throws UnableToRegisterIdempotentOperationException {
    return IdempotentLock.create(id, this);
  }

  @Override
  public IdempotentLock create(IdempotentId id, Duration timeout) throws UnableToRegisterIdempotentOperationException {
    return IdempotentLock.create(id, this, timeout);
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
      if (ErrorCategory.fromErrorCode(exception.getErrorCode()) == DUPLICATE_KEY) {
        Idempotent idempotent = wingsPersistence.get(Idempotent.class, id.getValue());
        return Response.builder().state(DONE).result((T) idempotent.getResult().get(0)).build();
      }
      throw new UnableToRegisterIdempotentOperationException(exception);
    } catch (RuntimeException exception) {
      throw new UnableToRegisterIdempotentOperationException(exception);
    }

    // If there was already record, but it was not successful, it is still running
    return Response.builder().state(RUNNING).build();
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
