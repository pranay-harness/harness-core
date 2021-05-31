package mongodb

import (
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/db"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/ti-service/cg"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.uber.org/zap"
	"os"
	"testing"
)

var svc cg.CgService
var mdb *db.MongoDb
var err error

func TestMain(m *testing.M) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mongoUri := os.Getenv("TEST_MONGO_URI")
	if mongoUri == "" {
		os.Exit(0)
	}
	mdb, err = db.NewMongoDb(
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
	svc = NewCgServiceImpl(*mdb, log.Sugar())
	os.Exit(m.Run())
}

func TestMongoDb_UploadPartialCgForNodes(t *testing.T) {
	ctx := context.Background()
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well
	// Setup nodes
	n1 := NewNode(1, "pkg", "m", "p", "c", "source",
		getVCSInfo(),
		"acct", "org", "proj")
	n2 := NewNode(2, "pkg", "m", "p", "c", "source",
		getVCSInfo(),
		"acct", "org", "proj")
	n := []interface{}{n1, n2}
	db.Database.Collection("nodes").InsertMany(ctx, n)

	// this should be added in nodes collection as ID: 3 is unique
	newNode := getNode(3)
	// this should be added in nodes collection as one entry already exist with ID 1
	nodeWithDuplicateId := getNode(1)
	cg := ti.Callgraph{
		Nodes: []ti.Node{newNode, nodeWithDuplicateId},
	}
	svc.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acct",
		"org",
		"proj",
		"target",
	)
	var nodes []Node
	curr, _ := mdb.Database.Collection("nodes").Find(ctx, bson.M{}, &options.FindOptions{})
	curr.All(ctx, &nodes)

	idSet := []int{1, 2, 3}
	assert.Equal(t, len(nodes), 3)
	for _, node := range nodes {
		fmt.Println(node.Id)
		assert.True(t, contains(idSet, node.Id))
	}

}

func TestMongoDb_UploadPartialCgForRelations(t *testing.T) {
	ctx := context.Background()
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	r1 := NewRelation(1, []int{1, 2}, getVCSInfo(), "acc", "org", "proj")
	r2 := NewRelation(2, []int{3, 4, 5, 6}, getVCSInfo(), "acc", "org", "proj")
	nodes := []interface{}{r1, r2}
	db.Database.Collection("relations").InsertMany(ctx, nodes)

	// this should be added in nodes collection as ID: 3 is unique
	newRelation := getRelation(3, []int{8})
	// this should be added in rel collection as one entry already exist with ID 1
	relWithDuplicateSrc := getRelation(1, []int{3, 2})
	cg := ti.Callgraph{
		Relations: []ti.Relation{newRelation, relWithDuplicateSrc},
	}
	svc.UploadPartialCg(ctx, &cg,
		getVCSInfo(),
		"acct",
		"org",
		"proj",
		"target",
	)
	var relations []Relation
	curr, _ := mdb.Database.Collection("relations").Find(ctx, bson.M{}, &options.FindOptions{})
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

// Change in a unsupported file (non java file) should select all the tests.
func Test_GetTestsToRun_Unsupported_File(t *testing.T) {
	ctx := context.Background()
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert sources and tests
	n1 := NewNode(1, "pkg1", "m1", "param", "cls1", "source",
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, "pkg1", "m2", "param", "cls1", "test",
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(2, "pkg2", "m1", "param", "cls1", "test",
		getVCSInfo(), "acct", "org", "proj")
	n4 := NewNode(2, "pkg2", "m2", "param", "cls1", "test",
		getVCSInfo(), "acct", "org", "proj")
	n5 := NewNode(2, "pkg2", "m1", "param", "cls2", "test",
		getVCSInfo(), "acct", "org", "proj")

	n := []interface{}{n1, n2, n3, n4, n5}
	db.Database.Collection("nodes").InsertMany(ctx, n)

	chFiles := []types.File{{Name: "a.xml", Status: types.FileModified}}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{Files: chFiles, TargetBranch: "branch", Repo: "repo"})
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, true)
	assert.Equal(t, resp.TotalTests, 4)
	assert.Equal(t, resp.SelectedTests, 4)
	assert.Equal(t, resp.SrcCodeTests, 4)
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
	ctx := context.Background()
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, "path.to.pkg", "m1", "param", "Abc", "source",
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, "path.to.test", "m2", "param", "AbcTest", "test",
		getVCSInfo(), "acct", "org", "proj")
	n3 := NewNode(3, "pkg2", "m2", "param", "cls1", "test",
		getVCSInfo(), "acct", "org", "proj")
	n4 := NewNode(4, "pkg2", "m1", "param", "cls2", "test",
		getVCSInfo(), "acct", "org", "proj")
	n5 := NewNode(5, "path.to.pkg2", "m1", "param", "Xyz", "source",
		getVCSInfo(), "acct", "org", "proj")
	n6 := NewNode(6, "path.to.test2", "m1", "param", "XyzTest", "test",
		getVCSInfo(), "acct", "org", "proj")
	n7 := NewNode(7, "path.to.test3", "m1", "param", "DefTest", "test",
		getVCSInfo(), "acct", "org", "proj")
	n8 := NewNode(8, "path.to.src4", "m1", "param", "Ghi", "source",
		getVCSInfo(), "acct", "org", "proj")
	n9 := NewNode(9, "path.to.test4", "m1", "param", "GhiTest", "test",
		getVCSInfo(), "acct", "org", "proj")
	n10 := NewNode(10, "path.to.test4", "m2", "param", "GhiTest", "test",
		getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2, n3, n4, n5, n6, n7, n8, n9, n10}
	db.Database.Collection("nodes").InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(5, []int{6}, getVCSInfo(), "acct", "org", "proj")
	r3 := NewRelation(8, []int{9}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection("relations").InsertMany(ctx, []interface{}{r1, r2, r3})

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

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{TiConfig: ticonfig, Files: chFiles, TargetBranch: "branch", Repo: "repo"})
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
	ctx := context.Background()
	dropNodes(ctx)
	dropRelations(ctx)
	defer dropNodes(ctx)     // drop nodes after the test is completed as well
	defer dropRelations(ctx) // drop relations after the test is completed as well

	// Insert source and tests
	n1 := NewNode(1, "path.to.pkg", "m1", "param", "Abc", "source",
		getVCSInfo(), "acct", "org", "proj")
	n2 := NewNode(2, "path.to.test", "m2", "param", "AbcTest", "test",
		getVCSInfo(), "acct", "org", "proj")
	n := []interface{}{n1, n2}
	db.Database.Collection("nodes").InsertMany(ctx, n)

	// Add relation between them
	r1 := NewRelation(1, []int{2}, getVCSInfo(), "acct", "org", "proj")
	r2 := NewRelation(5, []int{6}, getVCSInfo(), "acct", "org", "proj")
	r3 := NewRelation(8, []int{9}, getVCSInfo(), "acct", "org", "proj")
	db.Database.Collection("relations").InsertMany(ctx, []interface{}{r1, r2, r3})

	chFiles := []types.File{{Name: "src/a.xml", Status: types.FileModified},
		{Name: "src/b.jsp", Status: types.FileModified},
		{Name: "src/main/java/path/to/pkg2/XyzTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test8/NewTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test3/DefTest.java", Status: types.FileAdded},
		{Name: "src/test/java/path/to/test4/GhiTest.java", Status: types.FileAdded}}
	ticonfig := types.TiConfig{}
	ticonfig.Config.Ignore = []string{"**/*.xml", "**/*.jsp"}

	resp, err := db.GetTestsToRun(ctx, types.SelectTestsReq{TiConfig: ticonfig, Files: chFiles, TargetBranch: "branch", Repo: "repo"})
	assert.Nil(t, err)
	assert.Equal(t, resp.SelectAll, false)
	assert.Equal(t, resp.TotalTests, 1)    // new tests will get factored after CG
	assert.Equal(t, resp.SelectedTests, 0) // don't factor in new tests here. they will be upserted after uploading of PCG
	assert.Equal(t, resp.SrcCodeTests, 0)
	assert.Equal(t, resp.UpdatedTests, 0)
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
	for _, n := range s {
		if n == searchTerm {

			return true
		}
	}
	return false
}

func dropNodes(ctx context.Context) {
	db.Database.Collection("nodes").Drop(ctx)
}

func dropRelations(ctx context.Context) {
	db.Database.Collection("relations").Drop(ctx)
}

func getVCSInfo() cg.VCSInfo {
	return cg.VCSInfo{
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
