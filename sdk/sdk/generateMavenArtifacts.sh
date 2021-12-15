#!/bin/bash

# Fail on any error.
set -e

echo "Jacquard sdk version # " $1

MY_DIR=$(pwd)/build/outputs/gmaven/jacquard-sdk-$1
mkdir -p $MY_DIR

cd ~/.m2/repository/com/google/jacquard/jacquard-sdk/$1

POM=jacquard-sdk-$1.pom
AAR=jacquard-sdk-$1.aar

#Create checksum files
pom_sha1=(`shasum ./$POM`)
echo ${pom_sha1[0]} > $POM.sha1

aar_sha1=(`shasum ./$AAR`)
echo ${aar_sha1[0]} > $AAR.sha1

# Copy required files to a directory
cp $POM.sha1 $MY_DIR
cp $AAR.sha1 $MY_DIR
cp $POM $MY_DIR
cp $AAR $MY_DIR

#Create zip
cd $MY_DIR
cd ../
zip -r  jacquard-sdk-$1.zip ./jacquard-sdk-$1

#Copy zip to artifacts dir.
cp ./jacquard-sdk-$1.zip $KOKORO_ARTIFACTS_DIR

echo "################### Maven  artifacts generated ############################"
