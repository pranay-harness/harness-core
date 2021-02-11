use clap::Clap;
use glob::glob;
use lazy_static::lazy_static;
use regex::Regex;
use std::fs::{copy, metadata, remove_file, File};
use std::io::{Result, Write};

use crate::execute::{read_lines, MODULE_IMPORT, TARGET_MODULE_IMPORT};

/// An action to be executed
#[derive(Clap)]
pub struct ApplyTarget {
    #[clap(short, long)]
    path: String,

    #[clap(short, long)]
    target: String,
}

pub fn apply_target(opts: ApplyTarget) {
    let md = metadata(&opts.path).unwrap();

    if md.is_file() {
        match apply_target_to_class(&opts.path, &opts.target) {
            Ok(_) => {}
            Err(err) => {
                println!("Failed to move the class with error: {}", err);
            }
        }
    }

    if md.is_dir() {
        for entry in glob(&format!("{}/**/*.java", &opts.path)).expect("Failed to read glob pattern") {
            match entry {
                Ok(path) => apply_target_to_class(path.to_str().unwrap(), &opts.target).expect(""),
                Err(_) => (),
            }
        }
    }
}

lazy_static! {
    pub static ref IMPORT_STATEMENT_PATTERN: Regex = Regex::new(r"^import .*;$").unwrap();
    pub static ref CLASS_STATEMENT_PATTERN: Regex = Regex::new(r"^public (abstract )?(class|interface) ").unwrap();
}

fn apply_target_to_class(class_file: &str, target_module: &str) -> Result<()> {
    let lines = read_lines(class_file)?;

    let target_file = format!("{}.tmp", class_file);

    let mut target = File::create(&target_file)?;

    let mut imported = false;
    let mut class = false;

    for line in lines {
        let l = line?;

        if !imported && IMPORT_STATEMENT_PATTERN.is_match(&l) {
            writeln!(target, "{}", &MODULE_IMPORT)?;
            writeln!(target, "{}", &TARGET_MODULE_IMPORT)?;
            imported = true;
        }

        if !class && CLASS_STATEMENT_PATTERN.is_match(&l) {
            if !imported {
                writeln!(target, "{}", &MODULE_IMPORT)?;
                writeln!(target, "{}", &TARGET_MODULE_IMPORT)?;
                imported = true;
            }

            writeln!(target, "@TargetModule(Module._{})", target_module)?;
            class = true;
        }

        writeln!(target, "{}", &l)?;
    }

    target.flush()?;

    if class {
        copy(&target_file, &class_file)?;
    }
    remove_file(&target_file)?;

    Ok(())
}
