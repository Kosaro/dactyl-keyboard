(ns dactyl-keyboard.handler
  (:require [clojure.data.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :as middleware]
            [scad-clj.model :refer [pi]]
            [selmer.parser :refer [render-file]]
            [dactyl-keyboard.generator :as g])
  (:gen-class))

(defn parse-int [s]
  (Integer/parseInt s))

(defn parse-bool [s]
  (Boolean/valueOf s))

(defn home [_]
  (render-file "index.html" {}))

(defn example [_]
  (render-file "example.html" {}))

(defn api [_]
  (render-file "json-help.html" {}))

(defn manuform [_]
  (render-file "manuform.html" {:column-curvature    (range 12 22)
                                :tenting-angle       (range 15 6 -1)
                                :thumb-tenting-angle (range 24 15 -1)
                                :height-offset       (range 4 16 2)}))

(defn lightcycle [_]
  (render-file "lightcycle.html" {:column-curvature       (range 12 22)
                                  :tenting-angle          (range 12 6 -1)
                                  :thumb-tenting-angle    (range 24 -24 -1)
                                  :thumb-column-curvature (range 36 8 -1)
                                  :thumb-row-curvature    (range 36 8 -1)
                                  :height-offset          (range 10 36 2)}))

(defn generate-manuform [req]
  (let [p                           (:form-params req)
        param-ncols                 (parse-int (get p "keys.columns"))
        param-nrows                 (parse-int (get p "keys.rows"))
        param-thumb-count           (case (get p "keys.thumb-count")
                                      "two" :two
                                      "three" :three
                                      "four" :four
                                      "five" :five
                                      :six)
        param-last-row-count        (case (get p "keys.last-row")
                                      "zero" :zero
                                      "full" :full
                                      :two)
        switch-type                 (case (get p "keys.switch-type")
                                      "mx" :mx
                                      "alps" :alps
                                      :box)
        param-inner-column          (parse-bool (get p "keys.inner-column"))

        param-alpha                 (parse-int (get p "curve.alpha"))
        param-beta                  (parse-int (get p "curve.beta"))
        param-centercol             (parse-int (get p "curve.centercol"))
        param-tenting-angle         (parse-int (get p "curve.tenting"))

        param-use-external-holder   (parse-bool (get p "connector.external-holder"))
        param-trrs-connector        (parse-bool (get p "connector.trrs-connector"))
        param-use-promicro-usb-hole (parse-bool (get p "connector.usb-hole"))

        param-hotswap               (parse-bool (get p "form.hotswap"))
        param-stagger                 (parse-bool (get p "form.stagger"))
        param-keyboard-z-offset     (parse-int (get p "form.height-offset"))
        param-wide-pinky            (parse-bool (get p "form.wide-pinky"))
        param-wire-post             (parse-bool (get p "form.wire-post"))
        param-screw-inserts         (parse-bool (get p "form.screw-inserts"))

        param-show-keycaps          (parse-bool (get p "misc.keycaps"))
        param-wrist-rest            (parse-bool (get p "misc.wrist-rest"))
        param-integrated-wrist-rest (parse-bool (get p "misc.integrated-wrist-rest"))
        is-right?                   (parse-bool (get p "misc.right-side"))

        param-generate-plate        (get p "generate-plate")
        param-generate-wrist-rest   (get p "generate-wrist-rest")
        param-generate-json         (get p "generate-json")

        generate-plate?             (some? param-generate-plate)
        generate-wrist-rest?        (some? param-generate-wrist-rest)
        generate-json?              (some? param-generate-json)

        c                           {:configuration-nrows                  param-nrows
                                     :configuration-ncols                  param-ncols
                                     :configuration-thumb-count            param-thumb-count
                                     :configuration-last-row-count         param-last-row-count
                                     :configuration-switch-type            switch-type
                                     :configuration-use-inner-column?      param-inner-column

                                     :configuration-alpha                  (if generate-json? param-alpha (/ pi param-alpha))
                                     :configuration-beta                   (if generate-json? param-beta (/ pi param-beta))
                                     :configuration-centercol              param-centercol
                                     :configuration-tenting-angle          (if generate-json? param-tenting-angle (/ pi param-tenting-angle))
                                     :configuration-plate-projection?      generate-plate?

                                     :configuration-use-external-holder?   param-use-external-holder
                                     :configuration-use-promicro-usb-hole? param-use-promicro-usb-hole
                                     :configuration-use-trrs?              param-trrs-connector

                                     :configuration-use-hotswap?           param-hotswap
                                     :configuration-stagger?                 param-stagger
                                     :configuration-z-offset               param-keyboard-z-offset
                                     :configuration-show-caps?             param-show-keycaps
                                     :configuration-use-wide-pinky?        param-wide-pinky
                                     :configuration-use-wire-post?         param-wire-post
                                     :configuration-use-screw-inserts?     param-screw-inserts
                                     :configuration-use-wrist-rest?        param-wrist-rest
                                     :configuration-integrated-wrist-rest? param-integrated-wrist-rest}
        generated-file              (cond
                                      generate-plate? {:file      (g/generate-plate-dm c is-right?)
                                                       :extension "scad"}
                                      generate-wrist-rest? {:file      (g/generate-wrist-rest-dm c is-right?)
                                                            :extension "scad"}
                                      generate-json? {:file      (g/generate-json-dm c is-right?)
                                                      :extension "json"}
                                      :else {:file      (g/generate-case-dm c is-right?)
                                             :extension "scad"})]
    {:status  200
     :headers {"Content-Type"        "application/octet-stream"
               "Content-Disposition" (str "inline; filename=\"manuform." (get generated-file :extension) "\"")}
     :body    (get generated-file :file)}))

(defn generate-lightcycle [req]
  (let [p                         (:form-params req)
        param-ncols               (parse-int (get p "keys.columns"))
        param-use-numrow?         (parse-bool (get p "keys.num-row"))
        param-use-lastrow?        (parse-bool (get p "keys.last-row"))
        param-thumb-count         (case (get p "keys.thumb-count")
                                    "two" :two
                                    "three" :three
                                    "six" :six
                                    "eight" :eight
                                    :five)
        param-alpha               (parse-int (get p "curve.alpha"))
        param-beta                (parse-int (get p "curve.beta"))
        param-tenting-angle       (parse-int (get p "curve.tenting-angle"))
        param-thumb-alpha         (parse-int (get p "curve.thumb-alpha"))
        param-thumb-beta          (parse-int (get p "curve.thumb-beta"))
        param-thumb-tenting-angle (parse-int (get p "curve.thumb-tenting-angle"))

        param-hotswap             (parse-bool (get p "form.hotswap"))
        param-thumb-offset-x      (parse-int (get p "form.thumb-offset-x"))
        param-thumb-offset-y      (parse-int (get p "form.thumb-offset-y"))
        param-thumb-offset-z      (parse-int (get p "form.thumb-offset-z"))
        param-z-offset            (parse-int (get p "form.z-offset"))
        param-manuform-offset     (parse-bool (get p "form.manuform-offset"))
        param-use-border          (parse-bool (get p "form.border"))

        param-use-external-holder (parse-bool (get p "misc.external-holder"))
        param-screw-inserts       (parse-bool (get p "misc.screw-inserts"))
        param-show-keycaps        (parse-bool (get p "misc.show-keycaps"))
        is-right?                 (parse-bool (get p "misc.right-side"))

        param-generate-plate      (get p "generate-plate")
        param-generate-json       (get p "generate-json")

        generate-plate?           (some? param-generate-plate)
        generate-json?            (some? param-generate-json)

        c                         {:configuration-ncols                param-ncols
                                   :configuration-use-numrow?          param-use-numrow?
                                   :configuration-use-lastrow?         param-use-lastrow?
                                   :configuration-thumb-count          param-thumb-count
                                   :configuration-create-side-nub?     false
                                   :configuration-use-alps?            false
                                   :configuration-use-hotswap?         param-hotswap

                                   :configuration-alpha                (if generate-json? param-alpha (/ pi param-alpha))
                                   :configuration-beta                 (if generate-json? param-beta (/ pi param-beta))
                                   :configuration-tenting-angle        (if generate-json? param-tenting-angle (/ pi param-tenting-angle))
                                   :configuration-use-border?          param-use-border
                                   :configuration-manuform-offset?     param-manuform-offset
                                   :configuration-z-offset             param-z-offset
                                   :configuration-thumb-alpha          (if generate-json? param-thumb-alpha (/ pi param-thumb-alpha))
                                   :configuration-thumb-beta           (if generate-json? param-thumb-beta (/ pi param-thumb-beta))
                                   :configuration-thumb-tenting-angle  (if generate-json? param-thumb-tenting-angle (/ pi param-thumb-tenting-angle))
                                   :configuration-thumb-offset-x       (if generate-json? param-thumb-offset-x (- 0 param-thumb-offset-x))
                                   :configuration-thumb-offset-y       (if generate-json? param-thumb-offset-y (- 0 param-thumb-offset-y))
                                   :configuration-thumb-offset-z       param-thumb-offset-z
                                   :configuration-use-external-holder? param-use-external-holder
                                   :configuration-show-caps?           param-show-keycaps

                                   :configuration-use-screw-inserts?   param-screw-inserts}
        generated-file              (cond
                                      generate-plate? {:file (g/generate-plate-dl c is-right?)
                                                       :extension "scad"}
                                      generate-json? {:file (g/generate-json-dl c is-right?)
                                                      :extension "json"}
                                      :else {:file (g/generate-case-dl c is-right?)
                                             :extension "scad"})]
    {:status  200
     :headers {"Content-Type"        "application/octet-stream"
               "Content-Disposition" (str "inline; filename=\"lightcycle." (get generated-file :extension) "\"")}
     :body    (get generated-file :file)}))

(defn api-generate-manuform [{body :body}]
  (let [keys           (get body :keys)
        curve          (get body :curve)
        connector      (get body :connector)
        form           (get body :form)
        misc           (get body :misc)
        c              {:configuration-ncols                  (get keys :columns 5)
                        :configuration-nrows                  (get keys :rows 4)
                        :configuration-thumb-count            (keyword (get keys :thumb-count "six"))
                        :configuration-last-row-count         (keyword (get keys :last-row "two"))
                        :configuration-create-side-nub?       (get keys :nubs false)
                        :configuration-use-alps?              (get keys :alps false)
                        :configuration-use-inner-column?      (get keys :inner-column false)

                        :configuration-alpha                  (/ pi (get curve :alpha 12))
                        :configuration-beta                   (/ pi (get curve :beta 36))
                        :configuration-centercol              (get curve :centercol 4)
                        :configuration-tenting-angle          (/ pi (get curve :tenting 15))

                        :configuration-use-external-holder?   (get connector :external false)
                        :configuration-use-trrs?              (get connector :trrs false)
                        :configuration-use-promicro-usb-hole? (get connector :micro-usb false)

                        :configuration-use-hotswap?           (get form :hotswap false)
                        :configuration-ortho?                 (not (get form :stagger true))
                        :configuration-use-wide-pinky?        (get form :wide-pinky false)
                        :configuration-z-offset               (get form :height-offset 4)
                        :configuration-use-wire-post?         (get form :wire-post false)
                        :configuration-use-screw-inserts?     (get form :screw-inserts false)

                        :configuration-show-caps?             (get misc :keycaps false)
                        :configuration-use-wrist-rest?        (get misc :wrist-rest false)
                        :configuration-plate-projection?      (not (get misc :case true))
                        :configuration-integrated-wrist-rest? (get misc :integrated-wrist-rest false)}
        generated-scad (g/generate-case-dm c (get misc :right-side true))]
    {:status  200
     :headers {"Content-Type"        "application/octet-stream"
               "Content-Disposition" "inline; filename=\"manuform.scad\""}
     :body    generated-scad}))

(defn api-generate-lightcycle [{body :body}]
  (let [keys           (get body :keys)
        curve          (get body :curve)
        connector      (get body :connector)
        form           (get body :form)
        misc           (get body :misc)
        c              {:configuration-ncols                (get keys :columns 5)
                        :configuration-use-numrow?          (get keys :num-row false)
                        :configuration-use-lastrow?         (get keys :last-row false)
                        :configuration-thumb-count          (keyword (get keys :thumb-count "two"))
                        :configuration-create-side-nub?     false
                        :configuration-use-alps?            false

                        :configuration-alpha                (/ pi (get curve :alpha 12))
                        :configuration-beta                 (/ pi (get curve :beta 36))
                        :configuration-tenting-angle        (/ pi (get curve :tenting 12))
                        :configuration-thumb-alpha          (/ pi (get curve :thumb-alpha 12))
                        :configuration-thumb-beta           (/ pi (get curve :thumb-beta 36))
                        :configuration-thumb-tenting-angle  (/ pi (get curve :thumb-tenting 12))

                        :configuration-use-external-holder? (get connector :external false)

                        :configuration-use-border?          (get form :use-border true)
                        :configuration-use-hotswap?         (get form :hotswap false)
                        :configuration-manuform-offset?     (get form :manuform-offset false)
                        :configuration-thumb-offset-x       (- 0 (get form :thumb-offset-x 52))
                        :configuration-thumb-offset-y       (- 0 (get form :thumb-offset-y 45))
                        :configuration-thumb-offset-z       (get form :thumb-offset-z 27)
                        :configuration-z-offset             (get form :z-offset 10)

                        :configuration-use-screw-inserts?   (get misc :screw-inserts false)}
        generated-scad (g/generate-case-dl c (get misc :right-side true))]
    {:status  200
     :headers {"Content-Type"        "application/octet-stream"
               "Content-Disposition" "inline; filename=\"lightcycle.scad\""}}
    :body generated-scad))

(defroutes app-routes
  (GET "/" [] home)
  (GET "/example" [] example)
  (GET "/api" [] api)
  (GET "/manuform" [] manuform)
  (POST "/manuform" [] generate-manuform)
  (GET "/lightcycle" [] lightcycle)
  (POST "/lightcycle" [] generate-lightcycle)
  (POST "/api/manuform" [] api-generate-manuform)
  (POST "/api/lightcycle" [] api-generate-lightcycle)
  (route/resources "/")
  (route/not-found "not found"))

(def app
  (-> app-routes
      (middleware/wrap-json-body {:keywords? true})
      (middleware/wrap-json-response)
      (wrap-defaults api-defaults)))
