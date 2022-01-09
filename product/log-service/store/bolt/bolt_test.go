// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package bolt

import (
	"bytes"
	"context"
	"io/ioutil"
	"os"
	"path/filepath"
	"testing"
)

var noContext = context.Background()

func TestBolt(t *testing.T) {
	// create a temporary directory on the host filesystem
	// to store our temporary database.
	dir, err := ioutil.TempDir("", "logservice")
	if err != nil {
		t.Errorf("Cannot create temp directory: %s", err)
		return
	}
	defer os.RemoveAll(dir)

	// create the temporary database
	db, err := New(filepath.Join(dir, "test.db"))
	if err != nil {
		t.Errorf("Cannot create temp database: %s", err)
		return
	}
	defer db.Close()

	err = db.Upload(noContext, "1", bytes.NewBufferString("hello bolt"))
	if err != nil {
		t.Errorf("Cannot create database entry: %s", err)
		return
	}

	res, err := db.Download(noContext, "1")
	if err != nil {
		t.Errorf("Cannot create database entry: %s", err)
		return
	}

	raw, err := ioutil.ReadAll(res)
	if err != nil {
		t.Errorf("Cannot read database entry: %s", err)
		return
	}

	got, want := string(raw), "hello bolt"
	if got != want {
		t.Errorf("Want database entry %s, got %s", want, got)
	}

	_, err = db.Download(noContext, "0")
	if err == nil {
		t.Errorf("Want not found error when key not found")
		return
	}

	err = db.Delete(noContext, "1")
	if err != nil {
		t.Errorf("Cannot delete database entry: %s", err)
		return
	}
}
