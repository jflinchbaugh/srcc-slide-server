#!/usr/bin/env bb
(ns slide-server
  (:require
   [hiccup2.core :as html]
   [babashka.process :as proc]
   [taoensso.timbre :as log]
   [org.httpkit.server :as server]))

(def port 8080)
(def refresh-interval-ms (* 1000 60 60 6)) ;; 6 hours
(def title "Event Room")

(log/info "slide-server on" port)

(def style "
html {
  box-sizing: border-box;
  font-size: 24pt;
  background-color: white;
}

* {
  box-sizing: inherit;
}

body,
h1,
h2,
h3,
h4,
h5,
h6,
p,
ol,
ul {
  margin: 0;
  padding: 0;
  font-weight: normal;
}

body {
  -webkit-user-select: none;
  user-select: none;

.action {
  background-color: black;
  text-color: white;
  padding: 5ex;
  border-radius: 1ex;
  margin-bottom: 1ex;
}

.actions {
  padding: 1ex;
}

a, a:visited, a:active {
  color: white;
  text-decoration: none;
}
")

(defn index []
  {:status 200
   :headers {"Content-type" "text/html"}
   :body (str
           "<!DOCTYPE html>\n"
           (html/html
             [:html {:lang "en"}
              [:head
               [:title title]
               [:style style]]
              [:body
               [:h1 title]
               [:div.actions
                [:a {:href  "/logo"}
                  [:div.action "Logo"]]
                [:a {:href  "/events"}
                 [:div.action "Events"]]
                [:a {:href  "/refresh"}
                 [:div.action "Refresh Events"]]
                ]]]))})

(defn redirect [location]
  {:status 302
   :headers {"Location" location}})

(defn not-found []
  {:status 404
   :headers {"Content-type" "text/plain"}
   :body "Not Found"
   })

(def refresh-script "
  cd $HOME/Pictures/events
  pwd
  rm -f *.jpg *.png *.jpeg
  cp ../default/* .
  wget -nd -r -A jpg,png --backups=0 -e robots=off --no-check-certificate \\
    --no-parent https://www.srccpaart.org/billboard/
  exit 0
")

(defn infinite-loop [interval-ms function]
  (function)
  (future
    (Thread/sleep interval-ms)
    (infinite-loop interval-ms function))
  nil)

(defn refresh-events []
  (proc/shell "sh" "-c" refresh-script))

(defn app [req]
  (let [response (case (:uri req)
                   "/" (index)
                   "/app-identity" {:status 200
                                    :headers {"Content-type" "text/plain"
                                              "Access-Control-Allow-Origin" "*"}
                                    :body (str "slide-server " title)}
                   "/logo" (do
                             (log/info "linking logo")
                             (proc/shell
                               "sh"
                               "-c"
                               "ln -sfn $HOME/Pictures/logo $HOME/Pictures/show
                                killall eom")
                             (redirect "/"))

                   "/events" (do
                               (log/info "linking events")
                               (proc/shell
                                 "sh"
                                 "-c"
                                 "ln -sfn $HOME/Pictures/events $HOME/Pictures/show
                                  killall eom")
                               (redirect "/"))
                   "/refresh" (do
                               (log/info "linking events")
                               (refresh-events)
                               (redirect "/events"))
                   (not-found))]
      response))

(def server-shutdown (server/run-server app {:host "0.0.0.0" :port port}))

(infinite-loop refresh-interval-ms refresh-events)

(loop []
  (proc/shell
    "sh"
    "-c"
    "xset s 0 0; xset s off
     eom -s $HOME/Pictures/show
     exit 0")
  (recur))
