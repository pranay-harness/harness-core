package mongodb

import (
	"context"
	"fmt"
	"go.mongodb.org/mongo-driver/bson"
	"time"

	"github.com/kamva/mgm/v3"
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

func getDefaultConfig() *mgm.Config {
	// TODO: (vistaar) Decrease this to a reasonable value (1 second or so).
	// Right now queries are slow while running locally.
	return &mgm.Config{
		CtxTimeout: 15 * time.Second,
	}
}

type Relation struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Source  int     `json:"source" bson:"source"`
	Tests   []int   `json:"tests" bson:"tests"`
	Acct    string  `json:"account" bson:"account"`
	Proj    string  `json:"project" bson:"project"`
	Org     string  `json:"organization" bson:"organization"`
	VCSInfo VCSInfo `json:"vcs_info" bson:"vcs_info"`
}

type Node struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Package string  `json:"package" bson:"package"`
	Method  string  `json:"method" bson:"method"`
	Id      int     `json:"id" bson:"id"`
	Params  string  `json:"params" bson:"params"`
	Class   string  `json:"class" bson:"class"`
	Type    string  `json:"type" bson:"type"`
	Acct    string  `json:"account" bson:"account"`
	Proj    string  `json:"project" bson:"project"`
	Org     string  `json:"organization" bson:"organization"`
	VCSInfo VCSInfo `json:"vcs_info" bson:"vcs_info"`
}

const (
	NodeColl  = "nodes"
	RelnsColl = "relations"
)

// VCSInfo contains metadata corresponding to version control system details
type VCSInfo struct {
	Repo     string `json:"repo" bson:"repo"`
	Branch   string `json:"branch" bson:"branch"`
	CommitId string `json:"commit_id" bson:"commit_id"`
}

// NewNode creates Node object form given fields
func NewNode(id int, pkg, method, params, class, typ string, vcs VCSInfo, acc, org, proj string) *Node {
	return &Node{
		Id:      id,
		Package: pkg,
		Method:  method,
		Params:  params,
		Class:   class,
		Type:    typ,
		Acct:    acc,
		Org:     org,
		Proj:    proj,
		VCSInfo: vcs,
	}
}

// NewRelation creates Relation object form given fields
func NewRelation(source int, tests []int, vcs VCSInfo, acc, org, proj string) *Relation {
	return &Relation{
		Source:  source,
		Tests:   tests,
		Acct:    acc,
		Org:     org,
		Proj:    proj,
		VCSInfo: vcs,
	}
}

func New(username, password, host, port, dbName string, connStr string, log *zap.SugaredLogger) (*MongoDb, error) {
	// If any connStr is provided, use that
	if connStr == "" {
		connStr = fmt.Sprintf("mongodb://%s:%s/?connect=direct", host, port)
	}

	log.Infow("trying to connect to mongo", "connStr", connStr)
	ctx := context.Background()
	opts := options.Client().ApplyURI(connStr)
	if len(username) > 0 {
		credential := options.Credential{
			Username: username,
			Password: password,
		}
		opts = opts.SetAuth(credential)
	}
	err := mgm.SetDefaultConfig(getDefaultConfig(), dbName, opts)
	if err != nil {
		return nil, err
	}
	_, client, _, err := mgm.DefaultConfigs()
	if err != nil {
		return nil, err
	}

	// Ping mongo server to see if it's accessible. This is a requirement for startup
	// of TI service.
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
		return err
	}
	switch coll {
	// We can use reflection here but since we have only 2 classes currently, I did not wanted to
	// add  extra complexity and overhead
	case NodeColl:
		res := (results).(*[]Node)
		err = cur.All(ctx, res)
	case RelnsColl:
		res := (results).(*[]Relation)
		err = cur.All(ctx, res)
	default:
		return fmt.Errorf("find operation called with unknown collection %s", coll)
	}
	mdb.Log.Infow("find operation", "filter", f, "collection", coll, "query_time", time.Since(start))
	return err
}

// BulkWrite is wrapper over BulkWrite fn in mongo driver and logs time taken in the query
func (mdb *MongoDb) BulkWrite(coll string, ctx context.Context, operations []mongo.WriteModel, b *options.BulkWriteOptions) (*mongo.BulkWriteResult, error) {
	start := time.Now()
	res, err := mdb.Database.Collection(RelnsColl).BulkWrite(ctx, operations, &options.BulkWriteOptions{})
	if err != nil {
		return nil, err
	}
	mdb.Log.Infow("bulk operation", "collection", coll, "query_time", time.Since(start))
	return res, nil
}
