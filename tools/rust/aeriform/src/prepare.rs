use clap::Clap;
use std::fs::File;
use std::io::{Result, Write};

use crate::java_module::model_names;
use crate::repo::GIT_REPO_ROOT_DIR;

/// A sub-command to prepare the bazel rules that build
#[derive(Clap)]
pub struct Prepare {}

pub fn prepare(_: Prepare) {
    println!("preparing...");
    match generate() {
        Ok(_) => {}
        Err(err) => {
            println!("Failed to move the class with error: {}", err);
        }
    }
}

fn generate() -> Result<()> {
    let modules = model_names();

    let target_file = format!("{}/.aeriform/BUILD.bazel", GIT_REPO_ROOT_DIR.as_str());
    let mut target = File::create(&target_file)?;

    writeln!(target, "load(\"//:tools/bazel/aeriform.bzl\", \"aeriform\")")?;

    let mut vec = modules.keys().collect::<Vec<&String>>();
    vec.sort();

    for module in vec {
        writeln!(target, "")?;
        writeln!(target, "aeriform(\"{}\")", module)?;
    }

    Ok(())
}
