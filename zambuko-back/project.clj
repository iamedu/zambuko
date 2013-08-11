(defproject zambuko-back "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.5.1"]
                 [lib-noir "0.6.6"]
                 [compojure "1.1.5"]
                 [ring-server "0.2.8"]
                 [selmer "0.3.6"]
                 [com.taoensso/timbre "2.1.2"]
                 [com.postspectacular/rotor "0.1.0"]
                 [com.taoensso/tower "1.7.1"]
                 [markdown-clj "0.9.28"]
                 [com.novemberain/monger "1.5.0"]
                 [org.jclouds/jclouds-allblobstore "1.6.0"]
                 [org.hornetq/hornetq-core-client "2.3.3.Final"]
                 [com.cemerick/friend "0.1.5" :exclusions [com.google.inject/guice]]
                 [org.activiti/activiti-engine "5.13"]
                 [org.clojure/tools.nrepl "0.2.3"]]
  :plugins [[lein-ring "0.8.5"]]
  :repositories [["Alfresco Maven Repository" "https://maven.alfresco.com/nexus/content/groups/public/"]]
  :ring {:handler zambuko-back.handler/war-handler
         :init    zambuko-back.handler/init
         :destroy zambuko-back.handler/destroy}
  :profiles
  {:production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [ring/ring-devel "1.1.8"]]}}
  :min-lein-version "2.0.0")
