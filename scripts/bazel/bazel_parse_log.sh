#!/usr/bin/env bash

bazel build src/tools/execlog:parser
bazel-bin/src/tools/execlog/parser --log_path=$1 --output_path=$1.txt

grep -v "runner: \"remote cache hit\"" $1.txt > $1.tmp
mv $1.tmp $1.txt

grep -v "runner: \"worker\"" $1.txt > $1.tmp
mv $1.tmp $1.txt

grep -v "runner: \"darwin-sandbox\"" $1.txt > $1.tmp
mv $1.tmp $1.txt

grep -v "remote_cache_hit: true" $1.txt > $1.tmp
mv $1.tmp $1.txt

grep -v "value: \"/bin:/usr/bin:/usr/local/bin\"" $1.txt > $1.tmp
mv $1.tmp $1.txt

grep -v "Library/Caches/bazelisk/downloads/bazelbuild/bazel-4.0.0-darwin-x86_64" $1.txt > $1.tmp
mv $1.tmp $1.txt
