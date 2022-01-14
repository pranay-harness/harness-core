package mongodb

import (
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/types"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.uber.org/zap"
	"os"
	"testing"
)

var db *MongoDb
var err error

func TestMain(m *testing.M) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	mongoUri := os.Getenv("TEST_MONGO_URI_TI")
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
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well
	// Setup nodes
	n1 := NewNode(1, 1, "pkg", "m", "p", "c", "source", "", false, getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "pkg", "m", "p", "c", "source", "", false, getVCSInfo(), "acct", "org", "proj")
	oldNode := NewNode(10, 10, "pkg", "m", "p", "c", "source", "", false, getVCSInfoWithCommit("oldCommit"), "acct", "org", "proj")
	n := []interface{}{n1, n2, oldNode}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// this should be added in nodes collection as ID: 3 is unique
	newNode := getNode(3)
	// this should be added in nodes collection as one entry already exist with ID 1
	nodeWithDuplicateId := getNode(1)
	cg := cgp.Callgraph{
		Nodes: []cgp.Node{newNode, nodeWithDuplicateId},
	}
	db.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acct",
		"org",
		"proj",
		"target",
	)
	// todo change this wis manual deletion script
	// ttl script of mongo runs every 60 seconds.
	//time.Sleep(70 * time.Second)

	// Expectations --
	// - commitId of node `oldNode` is older so this node should be deleted.
	// - newNode is newly uploaded node with new commit so it will be added.
	// - node with name `nodeWithDuplicateId` already exists with same commit so it should not be added.

	var nodes []Node
	curr, _ := db.Database.Collection(nodeColl).Find(ctx, bson.M{}, &options.FindOptions{})
	curr.All(ctx, &nodes)

	idSet := []int{1, 2, 3, 10}

	// before calling fn there were 2 nodes.
	// In fn call one node with older commit will be removed. One with new node id will be added and one which already
	// exists will be skipped.
	for _, no := range nodes {
		fmt.Println(no)
	}
	assert.Equal(t, len(nodes), 4)
	for _, node := range nodes {
		assert.True(t, contains(idSet, node.Id))
	}

}

func TestMongoDb_MergeCgForNodes(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well
	// Setup nodes
	n1 := NewNode(1, 1, "pkg", "m", "p", "c", "source", "", false, getVCSInfoWithBranch("b1"), "acct", "org", "proj")
	n2 := NewNode(2, 2, "pkg", "m", "p", "c", "source", "", false, getVCSInfoWithBranch("b1"), "acct", "org", "proj")
	n3 := NewNode(3, 3, "pkg", "m", "p", "c", "source", "", false, getVCSInfoWithBranchAndCommit("commit1", "b2"), "acct", "org", "proj")
	n4 := NewNode(1, 1, "pkg", "m", "p", "c", "source", "", false, getVCSInfoWithBranchAndCommit("commit1", "b2"), "acct", "org", "proj")
	n := []interface{}{n1, n2, n3, n4}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Expectations
	// - `newNode` is new node uploaded with new commit and new node so it will be added.
	// -  node `nodeWithDuplicateId` already exists with same commit so it should not be added.
	var nodes []Node
	curr, _ := db.Database.Collection(nodeColl).Find(ctx, bson.M{"vcs_info.branch": "b1"}, &options.FindOptions{})
	curr.All(ctx, &nodes)

	assert.Equal(t, len(nodes), 2)

	mergeReq := types.MergePartialCgRequest{
		AccountId:    "acct",
		Repo:         "repo.git",
		TargetBranch: "b1",
		Diff:         types.DiffInfo{Sha: "commit1"},
	}
	db.MergePartialCg(ctx, mergeReq)

	curr, _ = db.Database.Collection(nodeColl).Find(ctx, bson.M{"vcs_info.branch": "b1"}, &options.FindOptions{})
	curr.All(ctx, &nodes)

	idSet := []int{1, 2, 3}
	// there were 2 nodes in destination branch
	// there were 2 nodes in src branch -- node id 3 was unique while node id 1 was duplicate
	// node id 3 will be move to destination branch from the src branch while the node id 1 will be skipped
	// finally there should be 3 nodes in the destination branch and 0 nodes in the src branch
	assert.Equal(t, len(nodes), 3)
	for _, node := range nodes {
		fmt.Println(node.Id)
		assert.True(t, contains(idSet, node.Id))
	}

	curr, _ = db.Database.Collection(nodeColl).Find(ctx, bson.M{"vcs_info.branch": "b2"}, &options.FindOptions{})
	curr.All(ctx, &nodes)
	assert.Equal(t, len(nodes), 1)
}

func TestMongoDb_MergePartialCgForTestRelations(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	r1 := NewRelation(1, []int{1, 2}, getVCSInfoWithBranch("b1"), "acct", "org", "proj")
	r2 := NewRelation(2, []int{3, 4, 5, 6}, getVCSInfoWithBranch("b1"), "acct", "org", "proj")
	r3 := NewRelation(3, []int{1, 2}, getVCSInfoWithBranchAndCommit("commit1", "b2"), "acct", "org", "proj")             // moved
	r4 := NewRelation(2, []int{1, 2, 3, 7, 8, 9}, getVCSInfoWithBranchAndCommit("commit1", "b2"), "acct", "org", "proj") // merged
	r5 := NewRelation(4, []int{1, 6, 3, 7}, getVCSInfoWithBranchAndCommit("commit1", "b2"), "acct", "org", "proj")

	nodes := []interface{}{r1, r2, r3, r4, r5}
	db.Database.Collection(relnsColl).InsertMany(ctx, nodes)

	// call merge-callgraph function
	mergeReq := types.MergePartialCgRequest{
		AccountId:    "acct",
		Repo:         "repo.git",
		TargetBranch: "b1",
		Diff:         types.DiffInfo{Sha: "commit1"},
	}
	db.MergePartialCg(ctx, mergeReq)

	var relations []Relation
	curr, _ := db.Database.Collection(relnsColl).Find(ctx, bson.M{"vcs_info.branch": "b1"}, &options.FindOptions{})
	curr.All(ctx, &relations)

	// Expectation:
	// node 1 wil remain unchanged
	// node 2 is in both dest and src branch so it will merged and moved to dest branch
	// node 3 is in dest branch and it should be moved to src branch
	// node 4 is in dest branch and it should be moved to src branch
	idSet := []int{1, 2, 3, 4}
	assert.Equal(t, len(relations), 4)
	for _, reln := range relations {
		assert.True(t, contains(idSet, reln.Source))
	}

	rel := filterRelations(1, relations)
	assert.Equal(t, len(rel.Tests), 2)
	assert.True(t, contains(rel.Tests, 1))
	assert.True(t, contains(rel.Tests, 2))

	// 2 should be the merged and contain {1, 2, 3, 4, 5, 6, 7, 8, 9}
	rel = filterRelations(2, relations)
	assert.Equal(t, len(rel.Tests), 9)
	assert.True(t, contains(rel.Tests, 3))
	assert.True(t, contains(rel.Tests, 1))

	// 3 should be moved directly
	rel = filterRelations(3, relations)
	assert.Equal(t, len(rel.Tests), 2)

	// 4 should also be moved directly
	rel = filterRelations(4, relations)
	assert.Equal(t, len(rel.Tests), 4)

	curr, _ = db.Database.Collection(relnsColl).Find(ctx, bson.M{"vcs_info.branch": "b2"}, &options.FindOptions{})
	curr.All(ctx, &relations)
	assert.Equal(t, len(relations), 1)
}

func TestMongoDb_UploadPartialCgForTestRelations(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	r1 := NewRelation(1, []int{1, 2}, getVCSInfo(), "acc", "org", "proj")
	r2 := NewRelation(2, []int{3, 4, 5, 6}, getVCSInfo(), "acc", "org", "proj")

	newNode := getNode(3) // we need it because if len(nodes) == 0, fn is not triggered
	nodes := []interface{}{r1, r2}
	db.Database.Collection(relnsColl).InsertMany(ctx, nodes)

	// this should be added in nodes collection as ID: 3 is unique
	newRelation := getRelation(3, []int{8})
	// this should be added in rel collection as one entry already exist with ID 1
	relWithDuplicateSrc := getRelation(1, []int{3, 2})
	cg := cgp.Callgraph{
		Nodes:         []cgp.Node{newNode},
		TestRelations: []cgp.Relation{newRelation, relWithDuplicateSrc},
	}
	db.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acc",
		"org",
		"proj",
		"target",
	)

	var relations []Relation
	curr, _ := db.Database.Collection(relnsColl).Find(ctx, bson.M{}, &options.FindOptions{})
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

func TestMongoDb_MergePartialCgForVisEdges(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropVisEdges(ctx)
	defer dropVisEdges(ctx) // drop vis_edges after the test is completed as well

	r1 := NewVisEdge(1, []int{1, 2}, "acct", "org", "proj", getVCSInfoWithBranch("b1"))
	r2 := NewVisEdge(2, []int{3, 4, 5, 6}, "acct", "org", "proj", getVCSInfoWithBranch("b1"))
	r3 := NewVisEdge(3, []int{1, 2}, "acct", "org", "proj", getVCSInfoWithBranchAndCommit("commit1", "b2"))             // moved
	r4 := NewVisEdge(2, []int{1, 2, 3, 7, 8, 9}, "acct", "org", "proj", getVCSInfoWithBranchAndCommit("commit1", "b2")) // merged
	r5 := NewVisEdge(4, []int{1, 6, 3, 7}, "acct", "org", "proj", getVCSInfoWithBranchAndCommit("commit1", "b2"))

	nodes := []interface{}{r1, r2, r3, r4, r5}
	db.Database.Collection(visColl).InsertMany(ctx, nodes)

	// call merge-callgraph function
	mergeReq := types.MergePartialCgRequest{
		AccountId:    "acct",
		Repo:         "repo.git",
		TargetBranch: "b1",
		Diff:         types.DiffInfo{Sha: "commit1"},
	}
	db.MergePartialCg(ctx, mergeReq)

	var edges []VisEdge
	curr, _ := db.Database.Collection(visColl).Find(ctx, bson.M{"vcs_info.branch": "b1"}, &options.FindOptions{})
	curr.All(ctx, &edges)

	// Expectation:
	// edge 1 wil remain unchanged
	// edge 2 is in both dest and src branch so it will merged and moved to dest branch
	// edge 3 is in dest branch and it should be moved to src branch
	// edge 4 is in dest branch and it should be moved to src branch
	idSet := []int{1, 2, 3, 4}
	assert.Equal(t, len(edges), 4)
	for _, reln := range edges {
		assert.True(t, contains(idSet, reln.Caller))
	}

	rel := filterEdges(1, edges)
	assert.Equal(t, len(rel.Callee), 2)
	assert.True(t, contains(rel.Callee, 1))
	assert.True(t, contains(rel.Callee, 2))

	// 2 should be the merged and contain {1, 2, 3, 4, 5, 6, 7, 8, 9}
	rel = filterEdges(2, edges)
	assert.Equal(t, len(rel.Callee), 9)
	assert.True(t, contains(rel.Callee, 3))
	assert.True(t, contains(rel.Callee, 1))

	// 3 should be moved directly
	rel = filterEdges(3, edges)
	assert.Equal(t, len(rel.Callee), 2)

	// 4 should also be moved directly
	rel = filterEdges(4, edges)
	assert.Equal(t, len(rel.Callee), 4)

	curr, _ = db.Database.Collection(visColl).Find(ctx, bson.M{"vcs_info.branch": "b2"}, &options.FindOptions{})
	curr.All(ctx, &edges)
	assert.Equal(t, len(edges), 1)
}

func TestMongoDb_UploadPartialCgForVisGraph(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropVisEdges(ctx)
	defer dropVisEdges(ctx) // drop edges after the test is completed as well

	r1 := NewVisEdge(1, []int{1, 2}, "acc", "org", "proj", getVCSInfo())
	r2 := NewVisEdge(2, []int{3, 4, 5, 6}, "acc", "org", "proj", getVCSInfo())
	edges := []interface{}{r1, r2}
	db.Database.Collection(visColl).InsertMany(ctx, edges)

	// this should be added in edge collection as ID: 3 is unique
	newRelation := getRelation(3, []int{8})
	// this should be added in vis_edges collection as one entry already exist with ID 1
	relWithDuplicateSrc := getRelation(1, []int{3, 2})

	newNode := getNode(3) // we need it because if there are no nodes, fn is not triggered and it wouldn't do anything
	cg := cgp.Callgraph{
		Nodes:        []cgp.Node{newNode},
		VisRelations: []cgp.Relation{newRelation, relWithDuplicateSrc},
	}
	db.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acc",
		"org",
		"proj",
		"target",
	)

	var visEdges []VisEdge
	curr, _ := db.Database.Collection(visColl).Find(ctx, bson.M{}, &options.FindOptions{})
	curr.All(ctx, &visEdges)

	idSet := []int{1, 2, 3}
	assert.Equal(t, len(visEdges), 3)
	for _, reln := range visEdges {
		assert.True(t, contains(idSet, reln.Caller))
	}

	// assert key tests for vis_edge collection:
	// 1 should be updated to {2, 3} + {1, 2} == {1, 2, 3}
	rel := filterEdges(1, visEdges)

	assert.Equal(t, len(rel.Callee), 3)
	assert.True(t, contains(rel.Callee, 1))
	assert.True(t, contains(rel.Callee, 2))
	assert.True(t, contains(rel.Callee, 3))

	// 2 should be the same as was created {3, 4, 5, 6}
	rel = filterEdges(2, visEdges)
	assert.Equal(t, len(rel.Callee), 4)

	// 3 should be the same as was created {8}
	rel = filterEdges(3, visEdges)
	assert.Equal(t, len(rel.Callee), 1)
}

// Change in a unsupported file (non java file) should select all the tests.
func Test_GetTestsToRun_Unsupported_File(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert sources and tests
	n1 := NewNode(1, 1, "pkg1", "m1", "param", "cls1", "source", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "pkg1", "m2", "param", "cls1", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, 3, "pkg2", "m1", "param", "cls1", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n4 := NewNode(4, 4, "pkg2", "m2", "param", "cls1", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n5 := NewNode(5, 5, "pkg2", "m1", "param", "cls2", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")

	n := []interface{}{n1, n2, n3, n4, n5}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	chFiles := []types.File{{Name: "a.xml", Status: types.FileModified}}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, true)
	assert.Equal(t, resp.TotalTests, 4)
	assert.Equal(t, resp.SelectedTests, 4)
	assert.Equal(t, resp.SrcCodeTests, 4)
}

// Change in a test file which is a helper method.
func Test_GetTestsToRun_WithHelperTestMethods(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert sources and tests
	n1 := NewNode(1, 1, "test.helper", "m1", "param", "TestObjectHelper", "source", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "test.actual", "m2", "param", "TestFile", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, 3, "test.other", "m2", "param", "TestFileOther", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")

	n := []interface{}{n1, n2, n3}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection(relnsColl).InsertMany(ctx, []interface{}{r1})

	chFiles := []types.File{{Name: "some/path/src/test/java/test/helper/TestObjectHelper.java", Status: types.FileModified}}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, false)
	assert.Equal(t, resp.TotalTests, 2)
	assert.Equal(t, resp.SelectedTests, 1)
	assert.Equal(t, resp.SrcCodeTests, 1)
	assert.Equal(t, resp.Tests[0].Class, "TestObjectHelper")
	assert.Equal(t, resp.Tests[0].Pkg, "test.helper")
	assert.Equal(t, resp.Tests[1].Class, "TestFile")
	assert.Equal(t, resp.Tests[1].Pkg, "test.actual")
}

// CG stored with different account and query with different account
func Test_GetTestsToRun_DifferentAccount(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert sources and tests
	n1 := NewNode(1, 1, "pkg1", "m1", "param", "cls1", "source", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "pkg1", "m2", "param", "cls1", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, 3, "pkg2", "m1", "param", "cls1", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n4 := NewNode(4, 4, "pkg2", "m2", "param", "cls1", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n5 := NewNode(5, 5, "pkg2", "m1", "param", "cls2", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")

	n := []interface{}{n1, n2, n3, n4, n5}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	chFiles := []types.File{{Name: "a.xml", Status: types.FileModified}}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "diffAct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, true)
	assert.Equal(t, resp.TotalTests, 0) // Nothing got returned
	assert.Equal(t, resp.SelectedTests, 0)
	assert.Equal(t, resp.SrcCodeTests, 0)
}

/* Test which passes modified, updated, deleted files and ti config
and checks response.

	Changes:
 		a.xml (modified)
 		Abc.java (modified)
		Xyz.java (modified)
		NewTest.java (added)
		DefTest.java (modified)
		Ghi.java (modified)
		GhiTest.java (deleted)
	TiConfig:
		**.xml

   Expected Return:
  		DefTest (Reason: updated)
		AbcTest (Reason: Abc was modified)
		NewTest (Reason: new test added)
		XyzTest (Reason: Xyz was deleted and XyzTest is not deleted)
 		Tests corresponding to Ghi.java should not be run (since GhiTest is deleted)
		a.xml should be ignored
*/
func Test_GetTestsToRun_TiConfig_Added_Deleted(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, 1, "path.to.pkg", "m1", "param", "Abc", "source", "", false, getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "path.to.test", "m2", "param", "AbcTest", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, 3, "pkg2", "m2", "param", "cls1", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n4 := NewNode(4, 4, "pkg2", "m1", "param", "cls2", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n5 := NewNode(5, 5, "path.to.pkg2", "m1", "param", "Xyz", "source", "", false, getVCSInfo(), "acct", "org", "proj")
	n6 := NewNode(6, 6, "path.to.test2", "m1", "param", "XyzTest", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n7 := NewNode(7, 7, "path.to.test3", "m1", "param", "DefTest", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n8 := NewNode(8, 8, "path.to.src4", "m1", "param", "Ghi", "source", "", false, getVCSInfo(), "acct", "org", "proj")
	n9 := NewNode(9, 9, "path.to.test4", "m1", "param", "GhiTest", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n10 := NewNode(10, 10, "path.to.test4", "m2", "param", "GhiTest", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2, n3, n4, n5, n6, n7, n8, n9, n10}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(5, []int{6}, getVCSInfo(), "acct", "org", "proj")
	r3 := NewRelation(8, []int{9}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection(relnsColl).InsertMany(ctx, []interface{}{r1, r2, r3})

	chFiles := []types.File{{Name: "src/a.xml", Status: types.FileModified},
		{Name: "src/b.jsp", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg/Abc.java", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg2/Xyz.java", Status: types.FileModified},
		{Name: "src/test/java/path/to/test8/NewTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test3/DefTest.java", Status: types.FileModified},
		{Name: "src/main/java/path/to/src4/Ghi.java", Status: types.FileModified},
		{Name: "src/test/java/path/to/test4/GhiTest.java", Status: types.FileDeleted}}
	ticonfig := types.TiConfig{}
	ticonfig.Config.Ignore = []string{"**/*.xml", "**/*.jsp"}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{TiConfig: ticonfig, Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, false)
	assert.Equal(t, resp.TotalTests, 7)
	assert.Equal(t, resp.SelectedTests, 3) // don't factor in new tests here. they will be upserted after uploading of PCG
	assert.Equal(t, resp.SrcCodeTests, 2)
	assert.Equal(t, resp.UpdatedTests, 1)
	assert.Contains(t, resp.Tests, types.RunnableTest{Pkg: "path.to.test8", Class: "NewTest", Method: "*", Selection: types.SelectNewTest})
	assert.Contains(t, resp.Tests, types.RunnableTest{Pkg: "path.to.test3", Class: "DefTest", Method: "m1", Selection: types.SelectUpdatedTest})
	assert.Contains(t, resp.Tests, types.RunnableTest{Pkg: "path.to.test", Class: "AbcTest", Method: "m2", Selection: types.SelectSourceCode})
	assert.Contains(t, resp.Tests, types.RunnableTest{Pkg: "path.to.test2", Class: "XyzTest", Method: "m1", Selection: types.SelectSourceCode})
}

func Test_GetTestsToRun_WithNewTests(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, 1, "path.to.pkg", "m1", "param", "Abc", "source", "", false, getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "path.to.test", "m2", "param", "AbcTest", "test", "", false, getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(5, []int{6}, getVCSInfo(), "acct", "org", "proj")
	r3 := NewRelation(8, []int{9}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection(relnsColl).InsertMany(ctx, []interface{}{r1, r2, r3})

	chFiles := []types.File{{Name: "src/a.xml", Status: types.FileModified},
		{Name: "src/b.jsp", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg2/XyzTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test8/NewTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test3/DefTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test4/GhiTest.java", Status: types.FileAdded}}
	ticonfig := types.TiConfig{}
	ticonfig.Config.Ignore = []string{"**/*.xml", "**/*.jsp"}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{TiConfig: ticonfig, Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, false)
	assert.Equal(t, resp.TotalTests, 1)    // new tests will get factored after CG
	assert.Equal(t, resp.SelectedTests, 0) // don't factor in new tests here. they will be upserted after uploading of PCG
	assert.Equal(t, resp.SrcCodeTests, 0)
	assert.Equal(t, resp.UpdatedTests, 0)
}

func Test_GetTestsToRun_WithNewTests_SameIds(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, 1, "path.to.pkg", "m1", "param", "Abc", "source", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 3, "path.to.test", "m2", "param", "AbcTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	// n3 and n4 have same IDs as n2. They should be ignored
	n3 := NewNode(2, 2, "path.to.test", "m2", "param", "AbcTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n4 := NewNode(2, 2, "path.to.test", "m2", "param", "AbcTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2, n3, n4}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(5, []int{6}, getVCSInfo(), "acct", "org", "proj")
	r3 := NewRelation(8, []int{9}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection(relnsColl).InsertMany(ctx, []interface{}{r1, r2, r3})

	chFiles := []types.File{{Name: "src/a.xml", Status: types.FileModified},
		{Name: "src/b.jsp", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg2/XyzTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test8/NewTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test3/DefTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test4/GhiTest.java", Status: types.FileAdded}}
	ticonfig := types.TiConfig{}
	ticonfig.Config.Ignore = []string{"**/*.xml", "**/*.jsp"}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{TiConfig: ticonfig, Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, false)
	assert.Equal(t, resp.TotalTests, 1)    // new tests will get factored after CG
	assert.Equal(t, resp.SelectedTests, 0) // don't factor in new tests here. they will be upserted after uploading of PCG
	assert.Equal(t, resp.SrcCodeTests, 0)
	assert.Equal(t, resp.UpdatedTests, 0)
}

func Test_GetTestsToRun_WithResources_PartialSelection(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, 1, "path.to.pkg", "m1", "param", "Abc", "source", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "path.to.test", "m2", "param", "AbcTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, 3, "path.to.test2", "m3", "param", "XyzTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	// n3 and n4 have same IDs as n2. They should be ignored
	n4 := NewNode(4, 4, "", "", "", "", "resource", "abc.json", false,
		getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2, n3, n4}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(4, []int{3}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection(relnsColl).InsertMany(ctx, []interface{}{r1, r2})

	chFiles := []types.File{{Name: "src/test/resources/data/abc.json", Status: types.FileModified},
		{Name: "src/test/resources/different/path/abc.json", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg/Abc.java", Status: types.FileModified},
		{Name: "src/abc.xml", Status: types.FileModified}}
	ticonfig := types.TiConfig{}
	ticonfig.Config.Ignore = []string{"**/*.xml"}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{TiConfig: ticonfig, Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, false)
	assert.Equal(t, resp.TotalTests, 2)    // new tests will get factored after CG
	assert.Equal(t, resp.SelectedTests, 2) // don't factor in new tests here. they will be upserted after uploading of PCG
	assert.Equal(t, resp.SrcCodeTests, 2)
	assert.Equal(t, resp.UpdatedTests, 0)
}

func Test_GetTestsToRun_WithResources_FullSelection(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, 1, "path.to.pkg", "m1", "param", "Abc", "source", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, 2, "path.to.test", "m2", "param", "AbcTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, 3, "path.to.test2", "m3", "param", "XyzTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	// n3 and n4 have same IDs as n2. They should be ignored
	n4 := NewNode(4, 4, "", "", "", "", "resource", "abc.json", false,
		getVCSInfo(), "acct", "org", "proj")
	n5 := NewNode(5, 5, "path.to.another.test", "m2", "param", "XyzTest", "test", "", false,
		getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2, n3, n4, n5}
	db.Database.Collection(nodeColl).InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(4, []int{3}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection(relnsColl).InsertMany(ctx, []interface{}{r1, r2})

	chFiles := []types.File{{Name: "src/test/resources/data/abc.json", Status: types.FileModified},
		{Name: "src/test/resources/different/path/abc.json", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg/Abc.java", Status: types.FileModified},
		{Name: "src/test/resources/different/path/abc2.json", Status: types.FileModified}}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{Files: chFiles, TargetBranch: "branch", Repo: "repo.git"}, "acct", false)
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, true)
	assert.Equal(t, resp.TotalTests, 3)    // new tests will get factored after CG
	assert.Equal(t, resp.SelectedTests, 3) // don't factor in new tests here. they will be upserted after uploading of PCG
	assert.Equal(t, resp.SrcCodeTests, 3)
	assert.Equal(t, resp.UpdatedTests, 0)
}

/*
	Get the visualisation graph for a graph that looks like:
	1 -> 2 -> 3 -> ..... -> 50        51 -> 52 -> ...... -> 100
*/
func Test_VgSearch_LinearGraph(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	dropVisEdges(ctx)

	defer dropNodes(ctx)
	defer dropRelations(ctx)
	defer dropVisEdges(ctx)

	account := "account"
	org := "org"
	project := "project"
	pkg := "pkg"
	method := "method"

	var nodes []interface{}
	var edges []interface{}

	var expNodes []types.VisNode
	var expEdges []types.VisMapping

	// Create a graph 1 -> 2 -> 3 -> 4 -> .... 50      51 -> 52 ..... -> 100
	for i := 1; i <= 50; i++ {
		// Graph should be constructed on basis of class ID and not ID
		n := NewNode(0, i, pkg, method, "", fmt.Sprintf("cls%d", i), "source", "", false, getVCSInfo(), account, org, project)
		nodes = append(nodes, n)
		// Create an edge from i -> (i+1) if i != 50
		if i != 50 {
			e := NewVisEdge(i, []int{i + 1}, account, org, project, getVCSInfo())
			edges = append(edges, e)
		}
	}

	for i := 51; i <= 100; i++ {
		// Graph should be constructed on basis of class ID and not ID
		n := NewNode(0, i, pkg, method, "", fmt.Sprintf("cls%d", i), "source", "", false, getVCSInfo(), account, org, project)
		nodes = append(nodes, n)
		if i != 100 {
			e := NewVisEdge(i, []int{i + 1}, account, org, project, getVCSInfo())
			edges = append(edges, e)
		}
	}

	// Create nodes and edges
	db.Database.Collection(nodeColl).InsertMany(ctx, nodes)
	db.Database.Collection(visColl).InsertMany(ctx, edges)

	// Consruct expected response:
	for i := 20; i <= 50; i++ {
		vn := types.VisNode{Id: i, Package: pkg, Class: fmt.Sprintf("cls%d", i), Type: "source"}
		expNodes = append(expNodes, vn)
		if i != 50 { // There is no edge b/w 50 -> 51
			vm := types.VisMapping{From: i, To: []int{i + 1}}
			expEdges = append(expEdges, vm)
		}
	}

	// If nothing was found in source branch, it should use the target branch
	resp, err := db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch,
		Class: "pkg.cls20", Limit: 500, DiffFiles: []types.File{{Name: "src/main/java/pkg/cls40.java", Status: types.FileModified}}})
	assert.Nil(t, err)
	setImportance(expNodes, []int{40}, true)
	setRoot(expNodes, []int{20}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes)
	assert.ElementsMatch(t, resp.Edges, expEdges)
	setImportance(expNodes, []int{40}, false)
	setRoot(expNodes, []int{40}, false)

	// If elements are found in the source branch, we should use that
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: getVCSInfo().Branch, TargetBranch: "test",
		Class: "pkg.cls20", Limit: 500, DiffFiles: []types.File{{Name: "src/main/java/pkg/cls20.java", Status: types.FileModified}}})
	assert.Nil(t, err)
	setImportance(expNodes, []int{20}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes)
	assert.ElementsMatch(t, resp.Edges, expEdges)
	setImportance(expNodes, []int{20}, false)

	// If Limit is set to x, response should only contain information about x nodes
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: getVCSInfo().Branch, TargetBranch: "test",
		Class: "pkg.cls20", Limit: 15, DiffFiles: []types.File{{Name: "src/main/java/pkg/cls20.java", Status: types.FileModified}}})
	assert.Nil(t, err)
	setImportance(expNodes, []int{20}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes[:15])
	assert.ElementsMatch(t, resp.Edges, expEdges[:15])
	setImportance(expNodes, []int{20}, false)

	// If no class is specified as input, the expected behavior is to return the entire callgraph
	// (upto whatever limit is specified)
	var expNodesFull []types.VisNode
	var expEdgesFull []types.VisMapping
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch, Limit: 10000,
		DiffFiles: []types.File{{Name: "src/main/java/pkg/cls20.java", Status: types.FileModified},
			{Name: "src/main/java/pkg/cls1.java", Status: types.FileAdded},
			{Name: "src/main/java/pkg/cls40.java", Status: types.FileDeleted}}})
	for i := 1; i <= 100; i++ {
		vn := types.VisNode{Id: i, Package: pkg, Class: fmt.Sprintf("cls%d", i), Type: "source"}
		expNodesFull = append(expNodesFull, vn)
		if i != 50 && i != 100 { // There is no edge b/w 50 -> 51 and 100 -> 101
			vm := types.VisMapping{From: i, To: []int{i + 1}}
			expEdgesFull = append(expEdgesFull, vm)
		}
	}
	assert.Nil(t, err)
	setImportance(expNodesFull, []int{20, 1, 40}, true)
	setRoot(expNodesFull, []int{1}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodesFull)
	assert.ElementsMatch(t, resp.Edges, expEdgesFull)
	setImportance(expNodesFull, []int{20, 1, 40}, false)
	setRoot(expNodesFull, []int{1}, false)

	// Full search with a limit
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch, Limit: 76})
	for i := 1; i <= 100; i++ {
		vn := types.VisNode{Id: i, Package: pkg, Class: fmt.Sprintf("cls%d", i), Type: "source"}
		expNodesFull = append(expNodesFull, vn)
		if i != 50 && i != 100 { // There is no edge b/w 50 -> 51 and 100 -> 101
			vm := types.VisMapping{From: i, To: []int{i + 1}}
			expEdgesFull = append(expEdgesFull, vm)
		}
	}
	assert.Nil(t, err)
	setRoot(expNodesFull, []int{1}, true)
	assert.Equal(t, resp.Nodes, expNodesFull[:76])
	assert.Equal(t, resp.Edges, expEdgesFull[:75]) // Edge b/w 50 -> 51 does not exist
	setRoot(expNodesFull, []int{1}, false)
}

/*
	Get the visualisation graph for a fully connected graph with x nodes
//*/
func Test_VgSearch_FullyConnected(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	dropVisEdges(ctx)

	defer dropNodes(ctx)
	defer dropRelations(ctx)
	defer dropVisEdges(ctx)

	var expNodes []types.VisNode
	var expEdges []types.VisMapping

	account := "account"
	org := "org"
	project := "project"
	pkg := "pkg"
	method := "method"

	x := 100 // Max no. of nodes and edges

	var nodes []interface{}
	var edges []interface{}

	// Create a fully connected graph with x nodes
	for i := 1; i <= x; i++ {
		n := NewNode(i, i, pkg, method, "", fmt.Sprintf("cls%d", i), "source", "", false, getVCSInfo(), account, org, project)
		nodes = append(nodes, n)
		vn := types.VisNode{Id: i, Package: pkg, Class: fmt.Sprintf("cls%d", i), Type: "source"}
		expNodes = append(expNodes, vn)
		// Create an edge from i -> (j != i)
		var callee []int
		for j := 1; j <= x; j++ {
			if j != i {
				callee = append(callee, j)
			}
		}
		e := NewVisEdge(i, callee, account, org, project, getVCSInfo())
		edges = append(edges, e)
		vm := types.VisMapping{From: i, To: callee}
		expEdges = append(expEdges, vm)
	}

	// Create nodes and edges
	db.Database.Collection(nodeColl).InsertMany(ctx, nodes)
	db.Database.Collection(visColl).InsertMany(ctx, edges)

	// Any source node should return the full callgraph
	resp, err := db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch,
		Class: "pkg.cls1", Limit: 500,
		DiffFiles: []types.File{{Name: "src/main/java/pkg/cls5.java", Status: types.FileModified},
			{Name: "src/main/java/pkg/cls7.java", Status: types.FileModified}}})
	assert.Nil(t, err)
	setImportance(expNodes, []int{5, 7}, true)
	setRoot(expNodes, []int{1}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes)
	assert.ElementsMatch(t, resp.Edges, expEdges)
	setImportance(expNodes, []int{5, 7}, false)
	setRoot(expNodes, []int{1}, false)

	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch,
		Class: fmt.Sprintf("pkg.cls%d", x), Limit: 10000,
		DiffFiles: []types.File{{Name: fmt.Sprintf("src/main/java/pkg/cls%d.java", x), Status: types.FileModified}}})
	assert.Nil(t, err)
	setImportance(expNodes, []int{x}, true)
	setRoot(expNodes, []int{x}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes)
	assert.ElementsMatch(t, resp.Edges, expEdges)
	setImportance(expNodes, []int{x}, false)
	setImportance(expNodes, []int{x}, false)

	// Graph search with a limit
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch,
		Class: fmt.Sprintf("pkg.cls%d", x), Limit: 20})
	assert.Nil(t, err)
	setRoot(expNodes, []int{x}, true)
	assert.Equal(t, len(resp.Nodes), 20)
	assert.Equal(t, len(resp.Edges), 20)
	setRoot(expNodes, []int{x}, false)

	// Graph search with a class that doesn't exist
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch,
		Class: "pkg.cls2010", Limit: 20})
	assert.NotNil(t, err)

	// Graph search without providing a class
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch, Limit: 1000})
	assert.Nil(t, err)
	setRoot(expNodes, []int{1}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes)
	assert.ElementsMatch(t, resp.Edges, expEdges)
	setRoot(expNodes, []int{1}, true)
}

///*
//	Get the visualisation graph for a graph that looks like:
//	1 -> 2  3 -> 4 5 -> 6 ...... 99 -> 100
//*/
func Test_VgSearch_DisconnectedGraph(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ctx := logger.WithContext(context.Background(), log.Sugar())
	dropNodes(ctx)
	dropRelations(ctx)
	dropVisEdges(ctx)

	defer dropNodes(ctx)
	defer dropRelations(ctx)
	defer dropVisEdges(ctx)

	var expNodes []types.VisNode
	var expEdges []types.VisMapping

	account := "account"
	org := "org"
	project := "project"
	pkg := "pkg"
	method := "method"

	x := 100 // Max no. of nodes and edges

	var nodes []interface{}
	var edges []interface{}

	// Create the graph
	for i := 1; i <= x; i++ {
		n := NewNode(i, i, pkg, method, "", fmt.Sprintf("cls%d", i), "source", "", false, getVCSInfo(), account, org, project)
		nodes = append(nodes, n)
		vn := types.VisNode{Id: i, Package: pkg, Class: fmt.Sprintf("cls%d", i), Type: "source"}
		expNodes = append(expNodes, vn)
		// Create an edge from i -> i + 1 if i is odd
		if i&1 == 1 {
			e := NewVisEdge(i, []int{i + 1}, account, org, project, getVCSInfo())
			edges = append(edges, e)
			vm := types.VisMapping{From: i, To: []int{i + 1}}
			expEdges = append(expEdges, vm)
		}
	}

	// Create nodes and edges
	db.Database.Collection(nodeColl).InsertMany(ctx, nodes)
	db.Database.Collection(visColl).InsertMany(ctx, edges)

	// Search on class that exists
	resp, err := db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch,
		Class: "pkg.cls5", Limit: 500,
		DiffFiles: []types.File{{Name: "src/main/java/pkg/cls6.java", Status: types.FileModified}}})
	assert.Nil(t, err)
	assert.Equal(t, len(resp.Nodes), 2)
	assert.Equal(t, resp.Nodes[0].Id, 5)
	assert.Equal(t, resp.Nodes[1].Id, 6)
	assert.Equal(t, len(resp.Edges), 1)
	assert.Equal(t, resp.Edges[0].From, 5)
	assert.Equal(t, resp.Edges[0].To, []int{6})
	assert.Equal(t, resp.Nodes[0].Important, false)
	assert.Equal(t, resp.Nodes[1].Important, true)

	// Search without a class
	resp, err = db.GetVg(ctx, types.GetVgReq{AccountId: account, Repo: getVCSInfo().Repo,
		SourceBranch: "test", TargetBranch: getVCSInfo().Branch, Limit: 500})
	assert.Nil(t, err)
	setRoot(expNodes, []int{1}, true)
	assert.ElementsMatch(t, resp.Nodes, expNodes)
	assert.ElementsMatch(t, resp.Edges, expEdges)
	setRoot(expNodes, []int{1}, false)
}

func setImportance(n []types.VisNode, imp []int, val bool) {
	for idx := range n {
		if contains(imp, n[idx].Id) {
			n[idx].Important = val
		}
	}
}

func setRoot(n []types.VisNode, roots []int, val bool) {
	for idx := range n {
		if contains(roots, n[idx].Id) {
			n[idx].Root = val
		}
	}
}

func filterRelations(src int, relations []Relation) Relation {
	for _, rel := range relations {
		if rel.Source == src {
			return rel
		}
	}
	return Relation{}
}

func filterEdges(src int, visEdges []VisEdge) VisEdge {
	for _, rel := range visEdges {
		if rel.Caller == src {
			return rel
		}
	}
	return VisEdge{}
}

func getRelation(src int, tests []int) cgp.Relation {
	return cgp.Relation{
		Source: src,
		Tests:  tests,
	}
}

func dropNodes(ctx context.Context) {
	db.Database.Collection(nodeColl).Drop(ctx)
}

func dropRelations(ctx context.Context) {
	db.Database.Collection(relnsColl).Drop(ctx)
}

func dropVisEdges(ctx context.Context) {
	db.Database.Collection(visColl).Drop(ctx)
}

func getVCSInfo() VCSInfo {
	return getCustomVCSInfo("repo.git", "branch", "commit")
}

func getCustomVCSInfo(repo, branch, commit string) VCSInfo {
	return VCSInfo{
		Repo:     repo,
		Branch:   branch,
		CommitId: commit,
	}
}

func getVCSInfoWithBranchAndCommit(commit, branch string) VCSInfo {
	return getCustomVCSInfo("repo.git", branch, commit)
}

func getVCSInfoWithBranch(branch string) VCSInfo {
	return getCustomVCSInfo("repo.git", branch, "commit")
}

func getVCSInfoWithCommit(commit string) VCSInfo {
	return getCustomVCSInfo("repo.git", "branch", commit)
}

func getNode(id int) cgp.Node {
	return cgp.Node{
		Package: "pkg",
		Method:  "m",
		ID:      id,
		Params:  "params",
		Class:   "class",
		Type:    "source",
	}
}
