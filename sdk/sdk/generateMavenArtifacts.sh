#!/bin/bash
#
# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.=
#
# Will only format or check swift files which are uncommited.

# Fail on any error.
set -e

echo "Jacquard sdk version # " $1

MY_DIR=$(pwd)/build/outputs/gmaven/jacquard-sdk-$1
mkdir -p $MY_DIR

cd ~/.m2/repository/com/google/jacquard/jacquard-sdk/$1

POM=jacquard-sdk-$1.pom
AAR=jacquard-sdk-$1.aar
SRC=jacquard-sdk-$1-sources.jar

#Create checksum files
pom_sha1=(`shasum ./$POM`)
echo ${pom_sha1[0]} > $POM.sha1

aar_sha1=(`shasum ./$AAR`)
echo ${aar_sha1[0]} > $AAR.sha1

src_jar_sha1=(`shasum ./$SRC`)
echo ${src_jar_sha1[0]} > $SRC.sha1

# Copy required files to a directory
cp $POM.sha1 $MY_DIR
cp $AAR.sha1 $MY_DIR
cp $SRC.sha1 $MY_DIR
cp $POM $MY_DIR
cp $AAR $MY_DIR
cp $SRC $MY_DIR

#Create zip
cd $MY_DIR
cd ../
zip -r  jacquard-sdk-$1.zip ./jacquard-sdk-$1

#Copy zip to artifacts dir.
cp ./jacquard-sdk-$1.zip $KOKORO_ARTIFACTS_DIR

echo "################### Maven  artifacts generated ############################"
