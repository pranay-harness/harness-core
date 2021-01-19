use crate::java_class::{populate_target_module, JavaClass};
use lazy_static::lazy_static;
use multimap::MultiMap;
use rayon::prelude::*;
use regex::Regex;
use std::collections::{HashMap, HashSet};
use std::iter::FromIterator;
use std::process::Command;

#[derive(Debug)]
pub struct JavaModule {
    pub name: String,
    pub index: i32,
    pub directory: String,
    pub jar: String,
    pub srcs: HashMap<String, JavaClass>,
}

pub fn modules() -> HashMap<String, JavaModule> {
    let output = Command::new("bazel")
        .args(&["query", "//...", "--output", "label_kind"])
        .output()
        .expect("not able to run bazel");

    if !output.status.success() {
        panic!("Command executed with failing error code");
    }

    let mut vec = String::from_utf8(output.stdout)
        .unwrap()
        .lines()
        .into_iter()
        .par_bridge()
        .map(|line| line.to_string())
        .filter(|target| target.starts_with("java_library rule "))
        .map(|target| target.split_whitespace().last().unwrap().to_string())
        .filter(|name| name.ends_with(":module") || name.ends_with(":test"))
        .collect::<Vec<String>>();

    &vec.sort();

    vec.par_iter()
        .map(|name| populate(name))
        .filter(|module| module.is_some())
        .map(|module| module.unwrap())
        .map(|module| (module.name.clone(), module))
        .collect::<HashMap<String, JavaModule>>()
}

fn class(path: &String) -> String {
    let main = "/src/main/java/";
    let pos = path.find(main);
    if pos.is_some() {
        path.chars()
            .skip(pos.unwrap() + main.len())
            .take(path.len() - main.len() - pos.unwrap() - ".java".len())
            .map(|ch| if ch == '/' { '.' } else { ch })
            .collect()
    } else {
        "".to_string()
    }
}

fn populate_srcs(name: &String, dependencies: MultiMap<String, String>) -> HashMap<String, JavaClass> {
    let mut split = name.split(":");
    let chunk = split.next().unwrap();
    let prefix = &format!("{}:", chunk);
    let directory = &format!("{}/", chunk.strip_prefix("//").unwrap());

    let output = Command::new("bazel")
        .args(&["query", &format!("labels(srcs, {})", name)])
        .output()
        .expect("not able to run bazel");

    if !output.status.success() {
        panic!("Command executed with failing error code");
    }

    String::from_utf8(output.stdout)
        .unwrap()
        .lines()
        .map(|line| line.to_string())
        .map(|line| line.replace(prefix, directory))
        .map(|line| (class(&line), line))
        .map(|tuple| {
            let target_module = populate_target_module(&tuple.1);
            let deps = dependencies.get_vec(&tuple.0);

            let class_dependencies: HashSet<String> = if deps.is_none() {
                HashSet::new()
            } else {
                HashSet::from_iter(dependencies.get_vec(&tuple.0).unwrap().iter().cloned())
            };
            (
                tuple.0.clone(),
                JavaClass {
                    name: tuple.0,
                    location: tuple.1,
                    dependencies: class_dependencies,
                    target_module: target_module,
                },
            )
        })
        .collect::<HashMap<String, JavaClass>>()
}

fn dependency_class(line: &String) -> (String, String) {
    let re = Regex::new(r"^\s+([^ ]+)\s+->\s+([^ ]+).*").unwrap();

    let captures = re.captures(line).expect(line);

    let mtch1 = captures.get(1).expect("");
    let mtch2 = captures.get(2).expect("");

    (
        mtch1.as_str().split('$').nth(0).unwrap().to_string(),
        mtch2.as_str().split('$').nth(0).unwrap().to_string(),
    )
}

fn is_harness_class(class: &String) -> bool {
    class.starts_with("io.harness.") || class.starts_with("software.wings.")
}

fn populate_dependencies(jar: &String) -> MultiMap<String, String> {
    let output = Command::new("jdeps")
        .args(&["-v", jar])
        .output()
        .expect("not able to run bazel");

    if !output.status.success() {
        panic!("Command executed with failing error code");
    }

    let result = String::from_utf8(output.stdout).unwrap();

    //println!("{:?}", result);

    result
        .lines()
        .map(|line| line.to_string())
        .filter(|line| line.starts_with("   "))
        .map(|s| dependency_class(&s))
        .filter(|tuple| is_harness_class(&tuple.0))
        .filter(|tuple| is_harness_class(&tuple.1))
        .filter(|tuple| !tuple.0.eq(&tuple.1))
        .collect::<MultiMap<String, String>>()
}

lazy_static! {
    static ref BAZEL_ROOT_DIR: String = String::from_utf8(
        Command::new("bazel")
            .args(&["info", "bazel-genfiles"])
            .output()
            .unwrap()
            .stdout
    )
    .unwrap()
    .trim()
    .to_string();
}

fn populate(name: &String) -> Option<JavaModule> {
    let mut split = name.split(":");
    let directory = split.next().unwrap().strip_prefix("//").unwrap().to_string();
    let target_name = split.next().unwrap();
    let jar = format!("{}/{}/lib{}.jar", BAZEL_ROOT_DIR.as_str(), directory, target_name);

    let dependencies = populate_dependencies(&jar);
    //println!("{} {:?}", name, dependencies);

    let srcs = populate_srcs(&name, dependencies);

    //println!("{:?}", srcs);

    if srcs.is_empty() {
        None
    } else {
        Some(JavaModule {
            name: name.clone(),
            index: directory
                .chars()
                .take(3)
                .collect::<String>()
                .parse::<i32>()
                .expect("the model index was ot resolved"),
            directory: directory,
            jar: jar,
            srcs: srcs,
        })
    }
}
