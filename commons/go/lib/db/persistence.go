// Persistence interface container functions to interact with any persistent store
package db

import (
	"context"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

type Persistence interface {
	// DeleteMany is wrapper over deleteMany fn in mongo driver and logs time taken in the query
	DeleteMany(coll string, ctx context.Context, f bson.M, d *options.DeleteOptions) (*mongo.DeleteResult, error)

	// InsertMany is wrapper over insertMany fn in mongo driver and logs time taken in the query
	InsertMany(coll string, ctx context.Context, add []interface{}) (*mongo.InsertManyResult, error)

	// UpdateMany is wrapper over updateMany fn in mongo driver and logs time taken in the query
	UpdateMany(coll string, ctx context.Context, f bson.M, update bson.M) (*mongo.UpdateResult, error)

	// Find is wrapper over Find fn in mongo driver and logs time taken in the query
	Find(coll string, ctx context.Context, f bson.M, results interface{}) error

	// BulkWrite is wrapper over BulkWrite fn in mongo driver and logs time taken in the query
	BulkWrite(coll string, ctx context.Context, operations []mongo.WriteModel, b *options.BulkWriteOptions) (*mongo.BulkWriteResult, error)
}
