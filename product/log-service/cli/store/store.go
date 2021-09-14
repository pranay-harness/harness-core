// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package store

import (
	"context"

	"gopkg.in/alecthomas/kingpin.v2"
)

var nocontext = context.Background()

// Register the store commands.
func Register(app *kingpin.Application) {
	cmd := app.Command("store", "storage commands")
	registerDownload(cmd)
	registerDownloadLink(cmd)
	registerUpload(cmd)
	registerUploadLink(cmd)
}
