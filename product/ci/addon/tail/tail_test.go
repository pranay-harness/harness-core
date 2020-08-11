package tail

import (
	"context"
	"io/ioutil"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
)

// Write "test" into a file "test.txt" with some additional fields.
// Ensure that the log comes out to stdout as {"msg": "test.txt", .. additionalFields}
func TestStartAfterFileCreation(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, observedLogs := logs.GetObservedLogger(zap.InfoLevel)

	m1 := make(map[string]string)
	m1["t1"] = "s1"
	m1["t2"] = "s2"

	tmpfile, err := ioutil.TempFile("", "test")
	if err != nil {
		t.Fatalf("Could not create temporary file")
	}
	defer os.Remove(tmpfile.Name()) // clean up

	content := []byte("sample1\nsample2\n")

	if _, err := tmpfile.Write(content); err != nil {
		t.Fatalf("Unable to write content to temporary file")
	}
	if err := tmpfile.Close(); err != nil {
		t.Fatalf("Unable to close temporary file")
	}

	in := &addonpb.StartTailRequest{
		FileName:         tmpfile.Name(),
		AdditionalFields: m1,
	}

	_, err = Start(ctx, in, log.Sugar())
	if err != nil {
		t.Fatalf("Could not start tailing")
	}

	in2 := &addonpb.StopTailRequest{
		FileName: tmpfile.Name(),
		Wait:     true,
	}

	_, err = Stop(ctx, in2, log.Sugar())
	if err != nil {
		t.Fatalf("Could not stop tailing")
	}

	obsLogs := observedLogs.All()

	logContext := obsLogs[0].Context
	logMessage := obsLogs[0].Entry.Message
	assert.Equal(t, logMessage, "sample1")
	assert.Equal(t, len(logContext), 2)
	flag := ((logContext[0].Key == "t1" && logContext[1].Key == "t2") ||
		(logContext[0].Key == "t2" && logContext[1].Key == "t1"))
	assert.Equal(t, flag, true)

	logContext = obsLogs[1].Context
	logMessage = obsLogs[1].Entry.Message
	assert.Equal(t, logMessage, "sample2")
	assert.Equal(t, len(logContext), 2)
	flag = ((logContext[0].Key == "t1" && logContext[1].Key == "t2") ||
		(logContext[0].Key == "t2" && logContext[1].Key == "t1"))
	assert.Equal(t, flag, true)
}

func TestStartBeforeFileCreation(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, observedLogs := logs.GetObservedLogger(zap.InfoLevel)

	m1 := make(map[string]string)
	m1["t1"] = "s1"
	m1["t2"] = "s2"

	fileName := "test_start_before_file_creation.txt"

	in := &addonpb.StartTailRequest{
		FileName:         fileName,
		AdditionalFields: m1,
	}

	_, err := Start(ctx, in, log.Sugar())
	if err != nil {
		t.Fatalf("Could not start tailing")
	}

	err = ioutil.WriteFile(fileName, []byte("sample\n"), 0644)
	if err != nil {
		t.Fatalf("Could not write to the file")
	}
	defer os.Remove(fileName)

	in2 := &addonpb.StopTailRequest{
		FileName: fileName,
		Wait:     true,
	}

	_, err = Stop(ctx, in2, log.Sugar())
	if err != nil {
		t.Fatalf("Could not stop tailing")
	}

	obsLogs := observedLogs.All()

	logContext := obsLogs[0].Context
	logMessage := obsLogs[0].Entry.Message
	assert.Equal(t, logMessage, "sample")
	assert.Equal(t, len(logContext), 2)
	flag := ((logContext[0].Key == "t1" && logContext[1].Key == "t2") ||
		(logContext[0].Key == "t2" && logContext[1].Key == "t1"))
	assert.Equal(t, flag, true)

}

func TestMultipleStartSameFile(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, observedLogs := logs.GetObservedLogger(zap.InfoLevel)

	m1 := make(map[string]string)
	m1["t1"] = "s1"
	m1["t2"] = "s2"

	tmpfile, err := ioutil.TempFile("", "test")
	if err != nil {
		t.Fatalf("Could not create temporary file")
	}
	defer os.Remove(tmpfile.Name()) // clean up

	content := []byte("sample\n")

	if _, err := tmpfile.Write(content); err != nil {
		t.Fatalf("Unable to write content to temporary file")
	}
	if err := tmpfile.Close(); err != nil {
		t.Fatalf("Unable to close temporary file")
	}

	in := &addonpb.StartTailRequest{
		FileName:         tmpfile.Name(),
		AdditionalFields: m1,
	}

	// Invoke multiple Start RPCs
	_, err = Start(ctx, in, log.Sugar())
	if err != nil {
		t.Fatalf("Could not start tailing")
	}
	_, err = Start(ctx, in, log.Sugar())
	if err != nil {
		t.Fatalf("Could not start tailing")
	}
	_, err = Start(ctx, in, log.Sugar())
	if err != nil {
		t.Fatalf("Could not start tailing")
	}

	in2 := &addonpb.StopTailRequest{
		FileName: tmpfile.Name(),
		Wait:     true,
	}

	_, err = Stop(ctx, in2, log.Sugar())
	if err != nil {
		t.Fatalf("Could not stop tailing")
	}

	obsLogs := observedLogs.All()

	logContext := obsLogs[0].Context
	logMessage := obsLogs[0].Entry.Message
	assert.Equal(t, logMessage, "sample")
	assert.Equal(t, len(logContext), 2)
	flag := ((logContext[0].Key == "t1" && logContext[1].Key == "t2") ||
		(logContext[0].Key == "t2" && logContext[1].Key == "t1"))
	assert.Equal(t, flag, true)

}

func TestStopFilenameDoesntExist(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	in2 := &addonpb.StopTailRequest{
		FileName: "random_file_to_test.txt",
		Wait:     true,
	}

	_, err := Stop(ctx, in2, log.Sugar())
	assert.Nil(t, err)
}

func TestMultipleStopSameFilename(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, observedLogs := logs.GetObservedLogger(zap.InfoLevel)

	m1 := make(map[string]string)
	m1["t1"] = "s1"
	m1["t2"] = "s2"

	tmpfile, err := ioutil.TempFile("", "test")
	if err != nil {
		t.Fatalf("Could not create temporary file")
	}
	defer os.Remove(tmpfile.Name()) // clean up

	content := []byte("sample\n")

	if _, err := tmpfile.Write(content); err != nil {
		t.Fatalf("Unable to write content to temporary file")
	}
	if err := tmpfile.Close(); err != nil {
		t.Fatalf("Unable to close temporary file")
	}

	in := &addonpb.StartTailRequest{
		FileName:         tmpfile.Name(),
		AdditionalFields: m1,
	}

	// Invoke multiple Start RPCs
	_, err = Start(ctx, in, log.Sugar())
	if err != nil {
		t.Fatalf("Could not start tailing")
	}

	in2 := &addonpb.StopTailRequest{
		FileName: tmpfile.Name(),
		Wait:     true,
	}

	_, err = Stop(ctx, in2, log.Sugar())
	assert.Nil(t, err)
	_, err = Stop(ctx, in2, log.Sugar())
	assert.Nil(t, err)

	obsLogs := observedLogs.All()

	logContext := obsLogs[0].Context
	logMessage := obsLogs[0].Entry.Message
	assert.Equal(t, logMessage, "sample")
	assert.Equal(t, len(logContext), 2)
	flag := ((logContext[0].Key == "t1" && logContext[1].Key == "t2") ||
		(logContext[0].Key == "t2" && logContext[1].Key == "t1"))
	assert.Equal(t, flag, true)
}
