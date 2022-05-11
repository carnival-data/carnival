# Neo4j

Carnival supports [Neo4j](https://neo4j.com/).

## Neo4j Community Server

Neo4j Commnity Server can be used to open a Carnival graph and view it using the interactive web based Neo4j Community Server interface.

### Install Neo4j Community Server
1. Download the community server version 3.5+ from the [Neo4j Download Center](https://neo4j.com/download-center/#community).
2. Follow the installation directions for your platform.

### Copy or Link the Graph Database

Under the Neo4j Community Server installation directory, there will be a `data/databases` directory, which is where the graph databases need to go.  If you are using MacOS or .nix, a sym-link can be used to make the Carnival graph database avaialble to Neo4j Community Server.  If you are using Windows, the Carival database directory will need to be copied to `data/databases` before being opened by Neo4j Community Server.

#### MacOS and .nix

Use `ln -s` to create sym-links to your carnival database directory.    Ex. `my-app -> /Users/me/data/my_app_home/data/graph/app`.

#### Windows

In your Carnival home directory, copy the entire `data/graph/app` directory to `data/databases` in the Neo4j Community Server directoery.

### Configure Neo4j Community Server

Point Neo4j Community Server to the graph database directory under `data/databases`.

1. Edit `conf/neo4j.conf`
2. Set `dbms.active_database=my-app` where `my-app` is the name of the directory or sym-link in `data/databases`.
3. Unless you have turned on Neo4j authentication for your Carnival graph, disable authentication in Neo4j Community Server by setting `dbms.security.auth_enabled=false`.

### Start Neo4j Community Server

Neo4j Community Server can be start/stop/restarted using the script `bin/neo4j`.  Run the script for usage instructions.

### Web Interface

Once started, the graph should be available via the Neo4j web interface at `http://localhost:7474/`.
