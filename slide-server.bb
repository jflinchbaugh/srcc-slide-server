#!/usr/bin/env bb

;; script to start a web server
;; to control the srcc slide show
;; used on in-house displays

(ns slide-server
  (:require
   [hiccup2.core :as html]
   [babashka.process :as proc]
   [taoensso.timbre :as log]
   [org.httpkit.server :as server]))

(def config {:port 8080
             :refresh-interval-ms (* 1000 60 60 6) ;; 6 hours
             :title "Event Room" })

(log/info "Starting slide-server on" (:port config))

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
               [:title (:title config)]
               [:style style]]
              [:body
               [:h1 (:title config)]
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
  rm -f *.jpg *.png *.jpeg
  cp ../default/* .
  wget -nd -r -A jpg,png --backups=0 -e robots=off --no-check-certificate \\
    --no-parent https://www.srccpaart.org/billboard/
  exit 0
")

(defn infinite-background-loop [interval-ms function]
  (function)
  (future
    (Thread/sleep interval-ms)
    (infinite-background-loop interval-ms function))
  nil)

(defn refresh-events []
  (proc/shell "sh" "-c" refresh-script))

(defn app [req]
  (let [response (case (:uri req)
                   "/" (index)
                   "/app-identity" {:status 200
                                    :headers {"Content-type" "text/plain"
                                              "Access-Control-Allow-Origin" "*"}
                                    :body (str "slide-server " (:title config))}
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

(def server-shutdown (server/run-server app {:host "0.0.0.0" :port (:port config)}))

;; periodically refresh images
(infinite-background-loop (:refresh-interval-ms config) refresh-events)

;; start and restart eye-of-gnome viewer
(loop []
  (proc/shell
    "sh"
    "-c"
    "xset s 0 0; xset s off
     eom -s $HOME/Pictures/show
     exit 0")
  (recur))
