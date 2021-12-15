We’re using [Orchid](https://orchid.run/) to generate a static website for Jacquard sdk. Orchid also
parses sdk java packages and integrates the JavaDoc into the website.

## Running Orchid

### Prerequisites :
Orchid does not generate api java docs with java8. Before running Orchid, make sure **java11** is
installed on your machine. If not installed, you can install `java11` either from the terminal by
running this command ```$sudo mule install jdk11``` or other convenient way for you.

### Run Development Server :
- Navigate to the ```sdk/sdk``` folder of the repo.
- In the terminal, run ```../gradlew :docs:orchidServe```.
- When successful, you should see below message :
```
Build Complete
Generated 122 pages in 12s 324ms

Webserver Running at http://localhost:8080
Hit [CTRL-C] to stop the server and quit Orchid
```
- Now open a browser and navigate to http://localhost:8080 to see your static website.

### Generate HTML Build:
- Update ```docs/build.gradle``` file to edit `baseUrl` so as to match to the base URL of the host
```
orchid {
   ...
   baseUrl = "/<Your-website-directory>/"
}
```
- In Terminal Navigate to the ```sdk/sdk``` folder of the repo.
- Run ``` $../gradlew :docs:orchidBuild```.
- When successful, copy all files from ```$<repo>/docs/build/docs/orchid/``` to `Your-website-directory`
 inside your web server deployment directory.
