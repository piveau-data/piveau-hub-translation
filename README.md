# piveau translation

Middleware for communikation to eTranslation service of the EU Commission.

## Table of Contents
* [Build and Install](#build-and-install)
* [Build with Docker](#build-with-docker)
* [Configuration](#Configuration)


## Build and Install
If you would like to run an image with docker of this instance,
see [Build with Docker](#build-with-docker)

Requirements:
* Git
* Maven 3
* Openjdk 12

```bash
git clone git@github.com:piveau-data/piveau-hub-translation.git
cd piveau-hub-translation
mvn install
```
## Build with Docker

If you prefer `docker-compose`, there is an executable example in the project directory.


First the setup:
* Clone repository
* Navigate into the cloned directory
* Create a configuration from the sample
`$ cp conf/config_sample.json conf/config.json`
* Edit the configuration for the environment (see [configuration](#configuration))
* Start the application `$ mvn package exec:java`
* Brose to [http://localhost:8080](http://localhost:8080)

If already is fine, you can build and run the docker image:
* Build
    * `$ mvn clean package`
    * `$ sudo docker build -t translation .`
* Run
    * `$ sudo docker run -p 8080:8080 -d translation`


## Configuration

You have to configure the `conf/config.json` to run the translations.
You can find an example in `conf/config_sample.json`.

* `TRANSLATION_SERVICE`: Only for the port number of this application.
* `DATABASE`: Parameters for the connection to a PostgreSQL Database.

```json
{
    "jdbc_driver": "<driver domain>",
    "jdbc_url": "<jdbc url with user, password and database name>",
    "jdbc_max_pool_size": "<number of the available poolsize (min. 100)>",
    "user": "<name of the database user>",
    "password": "<password string of the database user>",
    "sql_queries": "<path to sql queries>"
  }
```

* `E_TRANSLATION`: Parameters for a connection to eTranslation

```json
{
    "user": "<name of the etranslation user>",
    "application": "<name of the etranslation application>",
    "password": "<password string of the etranslation application>",
    "e_translation_url": "<url to etranslation endpoint>",
    "callback_url": "<callback url to receive translation>",
    "simultanous_translations": "<int of max. allowed simultanous translations>"
  }
```
