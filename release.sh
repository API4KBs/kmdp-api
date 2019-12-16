#!/bin/bash
set -x #echo on

##################################################
#### Verify

if [ "$#" -ne 2 ]; then
    echo "Usage ./release.sh {release_version} {next_version}"
    exit 2
fi

# Ensure to be on the develop branch
git checkout develop

##################################################
#### Release

# Prepare release branch
git checkout -b "rel_$1"

# Apply all necesssary version changes/fixtures
mvn versions:set -DnewVersion=$1
## cannot set parent version to a range with maven 3.3.9 / maven versions 2.7
## mvn versions:update-parent -DparentVersion=$1
gsed -i -r "/<parent>/,/<\/parent>/ s|<version>(.*)</version>|<version>$1</version> |" pom.xml

mvn versions:set-property -Dproperty=kmdp.archetype.version.resolved -DnewVersion="\${kmdp.archetype.version}"
mvn versions:set-property -Dproperty=org.omg.spec.API4KP.version -DnewVersion="\${api4kp.version}"

gsed -i -r "s|<archetypeVersion>(.*)</archetypeVersion>|<archetypeVersion>\$\{kmdp.archetype.version\}</archetypeVersion> |" pom.xml

cd properties
gsed -i -r "s|version=(.*)|version=$1|" *.properties
cd ..


# Ensure it builds!
mvn clean initialize -Prefresh
mvn clean install -Prelease # -Dmaven.local.repo=../repo
if [[ "$?" -ne 0 ]] ; then
  mvn versions:revert
  git checkout develop
  git branch -d "rel_$1"
  echo 'release failed';
  exit -1
fi


# Commit changes
git commit -am "Candidate release $1"

##################################################
#### Rebase

# Rebase to ensure continuity
git checkout develop
git rebase "rel_$1"


##################################################
#### Move on

# Revert all necesssary version changes/fixtures
mvn versions:set -DnewVersion=$2
## cannot set parent version to a range with maven 3.3.9 / maven versions 2.7
gsed -i -r "/<parent>/,/<\/parent>/ s|<version>([0-9]+\.[0-9]+\.[0-9]+)</version>|<version>[$1,$2]</version> |" pom.xml
## cannot set properties to a variable literal using maven versions
gsed -i -r "s|<kmdp.archetype.version.resolved>(.*)</kmdp.archetype.version.resolved>|<kmdp.archetype.version.resolved>\$\{edu.mayo.kmdp:kmdp-api-facade-generator:jar.version\}</kmdp.archetype.version.resolved> |" pom.xml
gsed -i -r "s|<org.omg.spec.API4KP.version>(.*)</org.omg.spec.API4KP.version>|<org.omg.spec.API4KP.version>\$\{org.omg.spec:API4KP:jar.version\}</org.omg.spec.API4KP.version> |" pom.xml

gsed -i -r "s|<archetypeVersion>(.*)</archetypeVersion>|<archetypeVersion>\$\{edu.mayo.kmdp:kmdp-openapi-spring-service-archetype:jar.version\}</archetypeVersion> |" pom.xml

cd properties
gsed -i -r "s|version=(.*)|version=$2|" *.properties
cd ..

# Commit changes
git commit -am "Start version $2+"