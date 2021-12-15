#!/bin/bash
#
# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.=
#
# Will only format or check swift files which are uncommited.

rootdir=`git rev-parse --show-toplevel`
cd $rootdir

sdk_path="$rootdir/sdk/sdk"
cloud_api_terms_path="$rootdir/docs/build/docs/orchid/wiki/cloud-api-terms/index.html"

post_process=no

case "$1" in
    "build")
        command=":docs:orchidBuild"
        post_process=yes
        if [ -z "$API_KEY" ]; then
            echo "Please set the API_KEY environment variable"
            exit 1
        fi
        ;;
    "serve")
        command=":docs:orchidServe"
        ;;
    *)
        echo "Possible arguments:"
        echo '  serve : Invokes `gradlew :docs:orchidServe` to serve generated documentation locally.'
        echo '  build : Invokes `gradlew :docs:orchidBuild` to generate documents (including post-processing).'
        exit 3
        ;;
esac

cd $sdk_path
../gradlew $command

if [ "$post_process" = "yes" ]
then
    sed -i "" "s/%TEMPORARY_API_KEY%/$API_KEY/" "$cloud_api_terms_path"
fi
