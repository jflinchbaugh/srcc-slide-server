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

(def config
    {:port 8080
     :refresh-interval-ms (* 1000 60 60 6) ;; 6 hours
     :title (or (first *command-line-args*) "SRCC")})


(def style "
html {
  box-sizing: border-box;
  background-color: white;
  font-size: 24pt;
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
}

.action {
  background-color: black;
  color: white;
  padding: 3ex;
  border-radius: 1ex;
  margin-bottom: 1ex;
  width: 100%;
  font-size: 2rem;
}

.actions {
  padding: 1ex;
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
              [:form {:action "/logo" :method "post"}
               [:button.action {:type "submit"} "Logo"]]
              [:form {:action "/gdrive" :method "post"}
               [:button.action {:type "submit"} "Google Drive"]]
              [:form {:action "/events" :method "post"}
               [:button.action {:type "submit"} "Events"]]
              [:form {:action "/refresh" :method "post"}
               [:button.action {:type "submit"} "Refresh"]]]]]))})

(defn redirect [location]
  {:status 302
   :headers {"Location" location}})

(defn with-redirect [location action]
    (action)
    (redirect location))

(defn not-found []
  {:status 404
   :headers {"Content-type" "text/plain"}
   :body "Not Found"})

(def refresh-script "
  cd $HOME/Pictures/events
  rm -f *.jpg *.png *.jpeg
  cp ../default/* .
  wget -nd -r -A jpg,png --backups=0 -e robots=off --no-check-certificate \\
    --no-parent https://www.srccpaart.org/billboard/

  echo 'Syncing Google Drive'
  cd $HOME/Pictures/gdrive
  rclone sync --timeout 10s --progress \\
    'srcc-gdrive':'SRCC Documents'/'Live Screen Images' .

  killall eom

  exit 0
")

(defn refresh-images []
  (log/info "refreshing images")
  (proc/shell "sh" "-c" refresh-script))

(defn start-infinite-background-loop [interval-ms function]
  (function)
  (future
    (Thread/sleep interval-ms)
    (start-infinite-background-loop interval-ms function))
  nil)

(def ^:const a-day (* 1000 60 60 24))

(defn hour->ms [h]
  (* 1000 60 60 h))

(defn minute->ms [m]
  (* 1000 60 m))

(defn ms->hour [ms]
  (/ ms 60 60 1000))

(defn delay-to-time-of-day [from time-of-day]
  (->
    from
    (* -1)
    (+ time-of-day)
    (mod (* 1000 60 60 24))))

(defn now [] (System/currentTimeMillis))

(defn start-at
  [time-of-day-ms function]
  (let [delay (delay-to-time-of-day (now) time-of-day-ms)]
    (log/info (str "Starting schedule in " delay "ms."))
    (future
      (Thread/sleep delay)
      (function))))

(defn activate-dir
  [dir]
  (log/info (str "linking " dir))
  (proc/shell
    "sh"
    "-c"
    (str
      "ln -sfn $HOME/Pictures/"
      dir
      " $HOME/Pictures/show;"
      " killall eom")))

(defn serve-identity []
  {:status 200
   :headers {"Content-type" "text/plain"
             "Access-Control-Allow-Origin" "*"}
   :body (str "slide-server " (:title config))})

(defn app [req]
  (case (:request-method req)
    :get (case (:uri req)
           "/" (index)
           "/app-identity" (serve-identity)
           (not-found))
    :post (case (:uri req)
            "/logo" (with-redirect "/" (partial activate-dir "logo"))
            "/gdrive" (with-redirect "/" (partial activate-dir "gdrive"))
            "/events" (with-redirect "/" (partial activate-dir "events"))
            "/refresh" (with-redirect "/" refresh-images)
            (not-found))
    (not-found)))

(log/info "Starting slide-server on" (:port config))

;; start the web server
(def server-shutdown
  (server/run-server app {:host "0.0.0.0" :port (:port config)}))

;; periodically refresh images
(start-infinite-background-loop
  (:refresh-interval-ms config) refresh-images)

;; reset to events overnight
(start-at
  (+ (hour->ms 7))
  (partial start-infinite-background-loop
    a-day (partial activate-dir "events")))

;; start and restart eye-of-gnome viewer
(loop []
  (proc/shell
    "sh"
    "-c"
    " xset s 0 0; xset s off
      eom -s $HOME/Pictures/show
      exit 0")
  (recur))
