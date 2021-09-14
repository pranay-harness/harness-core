// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package main

import (
	"github.com/wings-software/portal/product/log-service/cli"

	_ "github.com/joho/godotenv/autoload"
)

func main() {
	cli.Command()
}
