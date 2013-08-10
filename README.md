zambuko
=======

Zambuko is a Shona word meaning "bridges".

This is what this project is meant to be, a bridge between you and your desired webapp.

To help you get there, it includes:

* An AngularJS frontend (using yeoman)
* A clojure backend
* A very simple administration interface
* A very simple development environment

We can explain each of these a little more:

AngularJS frontend
------------------

Includes:

* AngularJS
* Bootstrap 3
* Codemirror
* jquery 1.9
* jStorage

Clojure backed
---------------

We have a few opinions on data..

* Luminus (ring)
* Riak CS for file storage (uses S3 API)
* Riak for K/V, include html storage
* MongoDB for data
* Embedded Activiti

Admin interface
---------------

* User administration
* Authentication and authorization
* Clojure REPL
* MongoDB javascript console
* File browser (RiakCS)

