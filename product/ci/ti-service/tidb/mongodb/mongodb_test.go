package mongodb

import (
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.uber.org/zap"
	"os"
	"sort"
	"testing"
)

var db *MongoDb
var err error

func TestMain(m *testing.M) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mongoUri := os.Getenv("TEST_MONGO_URI")
	if mongoUri == "" {
		os.Exit(0)
	}
	db, err = New(
		"",
		"",
		"",
		"27017",
		"ti-test",
		mongoUri,
		log.Sugar())
	if err != nil {
		fmt.Println(fmt.Sprintf("%v", err))
	}
	os.Exit(m.Run())
}

func TestMongoDb_UploadPartialCgForNodes(t *testing.T) {
	ctx := context.Background()
	setupNodes(ctx)
	// this should be added in nodes collection as ID: 3 is unique
	newNode := getNode(3)
	// this should be added in nodes collection as one entry already exist with ID 1
	nodeWithDuplicateId := getNode(1)
	cg := ti.Callgraph{
		Nodes: []ti.Node{newNode, nodeWithDuplicateId},
	}
	db.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acct",
		"org",
		"proj",
		"target",
	)
	var nodes []Node
	curr, _ := db.Database.Collection("nodes").Find(ctx, bson.M{}, &options.FindOptions{})
	curr.All(ctx, &nodes)

	idSet := []int{1, 2, 3}
	assert.Equal(t, len(nodes), 3)
	for _, node := range nodes {
		assert.True(t, contains(idSet, node.Id))
	}
}

func TestMongoDb_UploadPartialCgForRelations(t *testing.T) {
	ctx := context.Background()
	setupRelations(ctx)
	// this should be added in nodes collection as ID: 3 is unique
	newRelation := getRelation(3, []int{8})
	// this should be added in rel collection as one entry already exist with ID 1
	relWithDuplicateSrc := getRelation(1, []int{3, 2})
	cg := ti.Callgraph{
		Relations: []ti.Relation{newRelation, relWithDuplicateSrc},
	}
	db.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acct",
		"org",
		"proj",
		"target",
	)
	var relations []Relation
	curr, _ := db.Database.Collection("relations").Find(ctx, bson.M{}, &options.FindOptions{})
	curr.All(ctx, &relations)

	idSet := []int{1, 2, 3}
	assert.Equal(t, len(relations), 3)
	for _, reln := range relations {
		assert.True(t, contains(idSet, reln.Source))
	}

	// assert key tests for relations collection:
	// 1 should be updated to {2, 3} + {1, 2} == {1, 2, 3}
	rel := filterRelations(1, relations)
	assert.Equal(t, len(rel.Tests), 3)
	assert.True(t, contains(rel.Tests, 1))
	assert.True(t, contains(rel.Tests, 2))
	assert.True(t, contains(rel.Tests, 3))

	// 2 should be the same as was created {3, 4, 5, 6}
	rel = filterRelations(2, relations)
	assert.Equal(t, len(rel.Tests), 4)

	// 3 should be the same as was created {8}
	rel = filterRelations(3, relations)
	assert.Equal(t, len(rel.Tests), 1)
}

func filterRelations(src int, relations []Relation) Relation {
	for _, rel := range relations {
		if rel.Source == src {
			return rel
		}
	}
	return Relation{}
}

func getRelation(src int, tests []int) ti.Relation {
	return ti.Relation{
		Source: src,
		Tests:  tests,
	}
}

func contains(s []int, searchTerm int) bool {
	i := sort.SearchInts(s, searchTerm)
	return i < len(s) && s[i] == searchTerm
}

func setupRelations(ctx context.Context) {
	db.Database.Collection("relations").Drop(ctx)
	r1 := NewRelation(1, []int{1, 2}, getVCSInfo(), "acc", "org", "proj")
	r2 := NewRelation(2, []int{3, 4, 5, 6}, getVCSInfo(), "acc", "org", "proj")
	nodes := []interface{}{r1, r2}
	db.Database.Collection("relations").InsertMany(ctx, nodes)
}

func setupNodes(ctx context.Context) {
	db.Database.Collection("nodes").Drop(ctx)
	n1 := NewNode(1, "pkg", "m", "p", "c", "source",
		getVCSInfo(),
		"acct", "org", "proj")
	n2 := NewNode(2, "pkg", "m", "p", "c", "source",
		getVCSInfo(),
		"acct", "org", "proj")
	nodes := []interface{}{n1, n2}
	db.Database.Collection("nodes").InsertMany(ctx, nodes)
}

func getVCSInfo() VCSInfo {
	return VCSInfo{
		Repo:     "repo",
		Branch:   "branch",
		CommitId: "commit",
	}
}

func getNode(id int) ti.Node {
	return ti.Node{
		Package: "pkg",
		Method:  "m",
		ID:      id,
		Params:  "params",
		Class:   "class",
		Type:    "source",
	}
}
