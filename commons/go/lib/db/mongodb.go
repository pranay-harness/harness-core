// MongoDb implements persistence inteface for interacting with mongodb database
package db

import (
	"context"
	"github.com/pkg/errors"
	"go.mongodb.org/mongo-driver/bson"
	"time"

	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"

	"go.uber.org/zap"
)

type MongoDb struct {
	Client   *mongo.Client
	Database *mongo.Database
	Log      *zap.SugaredLogger
}

func NewMongoDb(username, password, host, port, dbName string, connStr string, log *zap.SugaredLogger) (*MongoDb, error) {
	ctx, cancel := context.WithTimeout(context.Background(), DEFAULT_TIMEOUT*time.Second)
	defer cancel()
	opts := ConfigureMongoClient(username, password, host, port, connStr)
	client, err := mongo.Connect(ctx, opts)
	if err != nil {
		return nil, err
	}

	// Ping mongo server to see if it's accessible. This is a requirement for startup
	// of TI service.
	log.Infow("trying to connect to mongo", "connStr", connStr)
	err = client.Ping(ctx, readpref.Primary())
	if err != nil {
		return nil, err
	}

	log.Infow("successfully pinged mongo server")
	return &MongoDb{Client: client, Database: client.Database(dbName), Log: log}, nil
}

// DeleteMany is wrapper over deleteMany fn in mongo driver and logs time taken in the query
func (mdb *MongoDb) DeleteMany(coll string, ctx context.Context, f bson.M, d *options.DeleteOptions) (*mongo.DeleteResult, error) {
	start := time.Now()
	res, err := mdb.Database.Collection(coll).DeleteMany(ctx, f, d)
	mdb.Log.Infow("deleteMany operation", "filter", f, "collection", coll, "query_time", time.Since(start))
	return res, err
}

// InsertMany is wrapper over insertMany fn in mongo driver and logs time taken in the query
func (mdb *MongoDb) InsertMany(coll string, ctx context.Context, add []interface{}) (*mongo.InsertManyResult, error) {
	start := time.Now()
	res, err := mdb.Database.Collection(coll).InsertMany(ctx, add)
	mdb.Log.Infow("insertMany operation", "collection", coll, "query_time", time.Since(start))
	return res, err
}

// UpdateMany is wrapper over updateMany fn in mongo driver and logs time taken in the query
func (mdb *MongoDb) UpdateMany(coll string, ctx context.Context, f bson.M, update bson.M) (*mongo.UpdateResult, error) {
	start := time.Now()
	res, err := mdb.Database.Collection(coll).UpdateMany(ctx, f, update)
	mdb.Log.Infow("updateMany operation", "filter", f, "collection", coll, "query_time", time.Since(start))
	return res, err
}

// Find is wrapper over Find fn in mongo driver and logs time taken in the query
func (mdb *MongoDb) Find(coll string, ctx context.Context, f bson.M, results interface{}) error {
	start := time.Now()
	cur, err := mdb.Database.Collection(coll).Find(ctx, f)
	if err != nil {
		return errors.Wrap(err, "failed to do find query in mongo")
	}
	err = cur.All(ctx, results)
	if err != nil {
		return errors.Wrap(err, "failed to do find query in mongo")
	}
	mdb.Log.Infow("find operation", "filter", f, "collection", coll, "query_time", time.Since(start))
	return nil
}

// BulkWrite is wrapper over BulkWrite fn in mongo driver and logs time taken in the query
func (mdb *MongoDb) BulkWrite(coll string, ctx context.Context, operations []mongo.WriteModel, b *options.BulkWriteOptions) (*mongo.BulkWriteResult, error) {
	start := time.Now()
	res, err := mdb.Database.Collection("relations").BulkWrite(ctx, operations, &options.BulkWriteOptions{})
	if err != nil {
		return nil, err
	}
	mdb.Log.Infow("bulk operation", "collection", coll, "query_time", time.Since(start))
	return res, nil
}
