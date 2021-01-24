use lazy_static::lazy_static;
use multimap::MultiMap;
use rayon::prelude::*;
use regex::Regex;
use std::collections::{HashMap, HashSet};
use std::process::Command;

use crate::java_class::{class_dependencies, external_class, populate_target_module, JavaClass};

#[derive(Debug)]
pub struct JavaModule {
    pub name: String,
    pub index: i32,
    pub directory: String,
    pub jar: String,
    pub srcs: HashMap<String, JavaClass>,
    pub dependencies: HashSet<String>,
}

pub fn modules() -> HashMap<String, JavaModule> {
    let output = Command::new("bazel")
        .args(&["query", "//...", "--output", "label_kind"])
        .output()
        .expect("not able to run bazel");

    if !output.status.success() {
        panic!("Command executed with failing error code");
    }

    let modules = String::from_utf8(output.stdout)
        .unwrap()
        .lines()
        .into_iter()
        .par_bridge()
        .map(|line| line.to_string())
        .filter(|target| target.starts_with("java_library rule "))
        .map(|target| target.split_whitespace().last().unwrap().to_string())
        .filter(|name| name.ends_with(":module") || name.ends_with(":test"))
        .collect::<HashSet<String>>();

    let data_collection_dsl = populate_from_external(
        "https/harness-internal-read%40harness.jfrog.io/artifactory/harness-internal",
        "io/harness/cv",
        "data-collection-dsl",
        "0.18-RELEASE",
    );

    let mut result: HashMap<String, JavaModule> = modules
        .par_iter()
        .map(|name| (name.clone(), populate_from_bazel(name, &modules)))
        .collect::<HashMap<String, JavaModule>>();

    result.insert("data-collection-dsl".to_string(), data_collection_dsl);

    result
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

fn populate_srcs(name: &str, dependencies: &MultiMap<String, String>) -> HashMap<String, JavaClass> {
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
            let class_dependencies = class_dependencies(&tuple.0, &dependencies);
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

fn populate_module_dependencies(name: &str, modules: &HashSet<String>) -> HashSet<String> {
    let output = Command::new("bazel")
        .args(&["query", &format!("labels(deps, {})", name)])
        .output()
        .expect("not able to run bazel");

    if !output.status.success() {
        panic!("Command executed with failing error code");
    }

    String::from_utf8(output.stdout)
        .unwrap()
        .lines()
        .map(|line| line.to_string())
        .filter(|name| modules.contains(name.as_str()))
        .collect::<HashSet<String>>()
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

fn populate_dependencies(jar: &String) -> (MultiMap<String, String>, HashSet<String>) {
    let all = jar_dependencies(&jar);

    (
        all.iter()
            .filter(|tuple| is_harness_class(&tuple.0))
            .filter(|tuple| is_harness_class(&tuple.1))
            .map(|tuple| (tuple.0.clone(), tuple.1.clone()))
            .collect::<MultiMap<String, String>>(),
        all.iter()
            .filter(|tuple| {
                tuple.1.eq("com.google.protobuf.UnknownFieldSet") || tuple.1.eq("io.grpc.stub.AbstractStub")
            })
            .map(|tuple| tuple.0.clone())
            .collect::<HashSet<String>>(),
    )
}

fn jar_dependencies(jar: &str) -> Vec<(String, String)> {
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
        .filter(|tuple| !tuple.0.eq(&tuple.1))
        .collect()
}

lazy_static! {
    static ref BAZEL_BAZEL_GENFILES_DIR: String = String::from_utf8(
        Command::new("bazel")
            .args(&["info", "bazel-genfiles"])
            .output()
            .unwrap()
            .stdout
    )
    .unwrap()
    .trim()
    .to_string();
    static ref BAZEL_OUTPUT_BASE_DIR: String = String::from_utf8(
        Command::new("bazel")
            .args(&["info", "output_base"])
            .output()
            .unwrap()
            .stdout
    )
    .unwrap()
    .trim()
    .to_string();
}

fn populate_from_bazel(name: &String, modules: &HashSet<String>) -> JavaModule {
    let mut split = name.split(":");
    let directory = split.next().unwrap().strip_prefix("//").unwrap().to_string();
    let target_name = split.next().unwrap();
    let jar = format!(
        "{}/{}/lib{}.jar",
        BAZEL_BAZEL_GENFILES_DIR.as_str(),
        directory,
        target_name
    );

    let module_dependencies = populate_module_dependencies(name, modules);

    let (dependencies, protos) = populate_dependencies(&jar);

    let mut srcs = populate_srcs(&name, &dependencies);
    //println!("{:?}", srcs);

    protos.iter().for_each(|class| {
        srcs.insert(class.to_string(), external_class(class, &dependencies));
    });

    JavaModule {
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
        dependencies: module_dependencies,
    }
}

fn populate_from_external(artifactory: &str, package: &str, name: &str, version: &str) -> JavaModule {
    let jar = format!(
        "{}/external/maven_harness/v1/{}/{}/{}/{}/{}-{}.jar",
        BAZEL_OUTPUT_BASE_DIR.as_str(),
        artifactory,
        package,
        name,
        version,
        name,
        version
    );

    let all = jar_dependencies(&jar);
    //println!("{} {:?}", name, dependencies);

    let dependencies = all
        .iter()
        .filter(|tuple| is_harness_class(&tuple.0))
        .filter(|tuple| is_harness_class(&tuple.1))
        .map(|tuple| (tuple.0.clone(), tuple.1.clone()))
        .collect::<MultiMap<String, String>>();

    let srcs = all
        .iter()
        .filter(|tuple| is_harness_class(&tuple.0))
        .map(|tuple| (tuple.0.to_string(), external_class(&tuple.0, &dependencies)))
        .collect();

    JavaModule {
        name: name.to_string(),
        index: 1000,
        directory: "n/a".to_string(),
        jar: jar,
        srcs: srcs,
        dependencies: HashSet::new(),
    }
}
