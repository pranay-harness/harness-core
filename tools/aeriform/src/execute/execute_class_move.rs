use clap::Clap;
use std::fs::{create_dir_all, remove_file, File};
use std::io::{self, BufRead, BufReader, Result, Write};
use std::path::Path;

use crate::java_class::TARGET_MODULE_PATTERN;
use crate::repo::GIT_REPO_ROOT_DIR;

const MODULE_IMPORT: &str = "import io.harness.annotations.dev.Module;";
const TARGET_MODULE_IMPORT: &str = "import io.harness.annotations.dev.TargetModule;";

/// An action to be executed
#[derive(Clap)]
pub struct MoveClass {
    #[clap(long)]
    from_module: String,

    #[clap(long)]
    from_location: String,

    #[clap(long)]
    to_module: String,
}

fn read_lines<P>(filename: P) -> io::Result<io::Lines<BufReader<File>>>
where
    P: AsRef<Path>,
{
    let file = File::open(filename)?;
    Ok(BufReader::new(file).lines())
}

pub fn move_class(opts: MoveClass) {
    let source_file = &format!(
        "{}/{}/{}",
        GIT_REPO_ROOT_DIR.as_str(),
        opts.from_module,
        opts.from_location
    );
    let target_file = &format!(
        "{}/{}/{}",
        GIT_REPO_ROOT_DIR.as_str(),
        opts.to_module,
        opts.from_location
    );

    println!("Moving class from {} to {}", source_file, target_file);

    match copy_class(source_file, target_file) {
        Ok(_) => {
            remove_file(&source_file).unwrap();
        }
        Err(err) => {
            println!("Failed to move the class with error: {}", err);
            remove_file(&target_file).unwrap();
        }
    }
}

fn copy_class(source_file: &String, target_file: &String) -> Result<()> {
    let lines = read_lines(source_file)?;

    let target_dir = Path::new(target_file).parent().unwrap();
    create_dir_all(target_dir)?;

    let mut target = File::create(target_file)?;

    for line in lines {
        let l = line?;

        if TARGET_MODULE_PATTERN.is_match(&l) {
            continue;
        }

        if MODULE_IMPORT.eq(&l) || TARGET_MODULE_IMPORT.eq(&l) {
            continue;
        }

        writeln!(target, "{}", &l)?;
    }

    target.flush()?;

    Ok(())
}
