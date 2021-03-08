package mongodb

import (
	"context"
	"fmt"
	"github.com/kamva/mgm/v3"
	"time"

	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.mongodb.org/mongo-driver/bson"
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
		CtxTimeout: 10 * time.Second,
	}
}

type Relation struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Source int   `json:"source" bson:"source"`
	Tests  []int `json:"tests" bson:"tests"`
}

type Node struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Package  string `json:"package" bson:"package"`
	Method   string `json:"method" bson:"method"`
	Id       int    `json:"id" bson:"id"`
	Params   string `json:"params" bson:"params"`
	Class    string `json:"class" bson:"class"`
	Type     string `json:"type" bson:"type"`
	Repo     string `json:"repo" bson:"repo"`
	CommitId string `json:"commit_id" bson:"commit_id"`
}

func NewNode(id int, pkg, method, params, class, typ string) *Node {
	return &Node{
		Id:      id,
		Package: pkg,
		Method:  method,
		Params:  params,
		Class:   class,
		Type:    typ,
	}
}

func NewRelation(source int, tests []int) *Relation {
	return &Relation{
		Source: source,
		Tests:  tests,
	}
}

func New(username, password, host, port, dbName string, log *zap.SugaredLogger) (*MongoDb, error) {
	connStr := fmt.Sprintf("mongodb://%s:%s/?connect=direct", host, port)
	log.Infow("trying to connect to mongo", "connStr", connStr)
	ctx := context.Background()
	credential := options.Credential{
		Username: username,
		Password: password,
	}
	opts := options.Client().ApplyURI(connStr).SetAuth(credential)
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
	return &MongoDb{Client: nil, Database: nil, Log: log}, nil
}

// queryHelper gets the tests that need to be run corresponding to the packages and classes
func (mdb *MongoDb) queryHelper(pkgs, classes []string) ([]types.Test, error) {
	if len(pkgs) != len(classes) {
		return nil, fmt.Errorf("Length of pkgs: %d and length of classes: %d don't match", len(pkgs), len(classes))
	}
	result := []types.Test{}
	// Query 1
	// Get nodes corresponding to the packages and classes
	nodes := []Node{}
	var allowedPairs []interface{}
	for idx, pkg := range pkgs {
		cls := classes[idx]
		allowedPairs = append(allowedPairs, bson.M{"package": pkg, "class": cls})
	}
	err := mgm.Coll(&Node{}).SimpleFind(&nodes, bson.M{"$or": allowedPairs})
	if err != nil {
		return nil, err
	}
	if len(nodes) == 0 {
		// Log error message for debugging if no nodes are found
		mdb.Log.Errorw("could not find any nodes corresponding to the pkgs and classes",
			"pkgs", pkgs, "classes", classes)
	}

	nids := []int{}
	for _, n := range nodes {
		nids = append(nids, n.Id)
	}

	// Query 2
	// Get unique test IDs corresponding to these nodes
	relations := []Relation{}
	err = mgm.Coll(&Relation{}).SimpleFind(&relations, bson.M{"source": bson.M{"$in": nids}})
	if err != nil {
		return nil, err
	}
	mtids := make(map[int]struct{})
	tids := []int{}
	for _, t := range relations {
		for _, tid := range t.Tests {
			if _, ok := mtids[tid]; !ok {
				tids = append(tids, tid)
				mtids[tid] = struct{}{}
			}
		}
	}

	// Query 3
	// Get test information corresponding to test IDs
	tnodes := []Node{}
	err = mgm.Coll(&Node{}).SimpleFind(&tnodes, bson.M{"id": bson.M{"$in": tids}, "type": "test"})
	if err != nil {
		return nil, err
	}
	if len(tnodes) != len(tids) {
		// Log error message for debugging if we don't find a test ID in the node list
		mdb.Log.Errorw("number of elements in test IDs and retrieved nodes don't match",
			"test IDs", tids, "nodes", tnodes, "length(test ids)", len(tids), "length(nodes)", len(tnodes))
	}
	for _, t := range tnodes {
		result = append(result, types.Test{Pkg: t.Package, Class: t.Class, Method: t.Method})
	}

	return result, nil
}

// isValid checks whether the test is valid or not
func isValid(t types.Test) bool {
	return t.Pkg != "" && t.Class != ""
}

func (mdb *MongoDb) GetTestsToRun(ctx context.Context, files []string) ([]types.Test, error) {
	// parse package and class names from the files
	nodes, err := utils.ParseFileNames(files)
	m := make(map[types.Test]struct{}) // Get unique tests to run
	l := []types.Test{}
	if err != nil {
		return nil, err
	}
	var pkgs []string
	var cls []string
	for _, node := range nodes {
		// A file which is not recognized. Need to add logic for handling these type of files
		if !utils.IsSupported(node) {
			// A list with a single empty element indicates that all tests need to be run
			return []types.Test{{}}, nil
		} else if utils.IsTest(node) {
			t := types.Test{Pkg: node.Pkg, Class: node.Class, Method: node.Method}
			if !isValid(t) {
				mdb.Log.Errorw("received test without pkg/class as input")
			} else {
				// Test is valid, add the test
				if _, ok := m[t]; !ok {
					l = append(l, t)
					m[t] = struct{}{}
				}
			}
		} else {
			// Source file
			pkgs = append(pkgs, node.Pkg)
			cls = append(cls, node.Class)
		}
	}
	tests, err := mdb.queryHelper(pkgs, cls)
	if err != nil {
		return nil, err
	}
	for _, t := range tests {
		if !isValid(t) {
			mdb.Log.Errorw("found test without pkg/class data in mongo")
		} else {
			// Test is valid, add the test
			if _, ok := m[t]; !ok {
				l = append(l, t)
				m[t] = struct{}{}
			}
		}
	}
	return l, nil
}
