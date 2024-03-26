(defproject social "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [jumblerg/ring-cors "3.0.0"]
                 [ring/ring-json "0.5.1"]
                 [nano-id "1.0.0"]
                 [garden "1.3.10"]
                 [faker "0.2.2"]
                 [clojure.java-time "1.4.2"]]

  :plugins [[lein-ring "0.12.5"]
            [lein-garden "0.3.0"]
            [lein-auto "0.1.3"]]

  :ring {:handler social.handler/app
         :nrepl {:start? true}}
  :garden {:builds [{:id "screen"
                     :source-paths ["src/"]
                     :stylesheet social.styles/screen
                     :compiler {:output-to "resources/public/css/main.css"
                                :pretty-print? true}}]}
  :auto {:default {:file-pattern #"\.(clj|cljx|cljc|edn)$"}}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.2"]]}})
