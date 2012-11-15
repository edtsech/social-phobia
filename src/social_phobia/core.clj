(ns social-phobia.core
  (:use [clj-webdriver.taxi]
        [clojure.java.io]
        [clojure.algo.monads]
        [clojure.core.incubator :only [-?>]])
  (:require [clj-yaml.core :as yaml])
  (:gen-class main true))

; TODO: Check monad laws
; TODO: Find proper name
; TODO: Find better syntax for domonad
(defmonad erno-m
          [
           m-result (fn m-result-erno [v] v)
           m-bind   (fn m-bind-erno [mv f]
                      (if (:error mv) mv (f mv)))
           ])

(defn error [selector]
  {:status :fail :error (str (first (vals selector)) " not found")})

(defn- replace-text
  "Find an input and replace a text in it."
  [selector text]
  (let [el (find-element selector)]
    (if (:webelement el)
      (-> el
        clear
        (input-text text))
      (error selector))))

(defn- safe-click [selector]
  (let [el (find-element selector)]
    (if (:webelement el)
      (click el)
      (error selector))))

(defn update-twitter-bio [bio network]
  (with-driver
    {:browser (bio :browser)}
    (let [auth (-> bio :networks network)]
      {network (domonad
                 erno-m
                 [_ (to "http://twitter.com/")
                  _ (input-text "#signin-email" (auth :login))
                  _ (input-text "#signin-password" (auth :pass))
                  _ (safe-click {:css ".submit.flex-table-btn"})
                  _ (to "http://twitter.com/settings/profile")
                  _ (implicit-wait 3000)
                  _ (replace-text {:css "#user_name"} (str (bio :first-name) " " (bio :last-name)))
                  _ (replace-text {:css "#user_location"} (bio :location))
                  _ (replace-text {:css "#user_url"} (bio :web))
                  _ (replace-text {:css "#user_description"} (bio :bio))
                  _ (safe-click {:css "#settings_save"})
                  _ (quit)]
                 {network "ok"})})))

(defn update-foursquare-bio [bio network]
  (with-driver
    {:browser (bio :browser)}
    (let [auth (-> bio :networks network)]
      {network (domonad
                 erno-m
                 [_ (to "https://foursquare.com/login?continue=%2F&clicked=true")
                  _ (input-text "#username" (auth :login))
                  _ (input-text "#password" (auth :pass))
                  _ (safe-click {:css "input.greenButton"})
                  _ (to "https://foursquare.com/settings/")
                  _ (replace-text {:css "#firstname"} (bio :first-name))
                  _ (replace-text {:css "#lastname"} (bio :last-name))
                  _ (replace-text {:css "#userEmail"} (bio :email))
                  _ (replace-text {:css "textarea.formStyle"} (bio :bio))
                  _ (replace-text {:css "#ht_id"} (bio :location))
                  _ (safe-click {:css "input.greenButton"})
                  _ (quit)]
                 {:status "ok"})})))

(defn update-github-bio [bio network]
  (with-driver
    {:browser (bio :browser)}
    (let [auth (-> bio :networks network)]
      {network (domonad
                 erno-m
                 [_ (to "https://github.com/login")
                  _ (input-text "#login_field" (auth :login))
                  _ (input-text "#password" (auth :pass))
                  _ (safe-click {:xpath "//input[@type='submit']"})
                  _ (to "https://github.com/settings/profile")
                  _ (replace-text {:xpath "//dl[@data-name='profile_name']//input"}
                                  (str (bio :first-name) " " (bio :last-name)))
                  _ (replace-text {:xpath "//dl[@data-name='profile_email']//input"} (bio :email))
                  _ (replace-text {:xpath "//dl[@data-name='profile_blog']//input"} (bio :web))
                  _ (replace-text {:xpath "//dl[@data-name='profile_company']//input"} (bio :company))
                  _ (replace-text {:xpath "//dl[@data-name='profile_location']//input"} (bio :location))
                  _ (replace-text {:xpath "//dl[@data-name='gravatar_email']//input"} (bio :email))
                  _ (safe-click {:css "button.button.classy.primary"})
                  _ (quit)]
                 {:status "ok"})})))

(defn update-instagram-bio [bio network]
  (with-driver
    {:browser (bio :browser)}
    (let [auth (-> bio :networks network)]
      {network (domonad
                 erno-m
                 [_ (to "https://instagram.com/accounts/login/?next=/accounts/edit/")
                  _ (replace-text {:css "#id_username"} (auth :login))
                  _ (replace-text {:css "#id_password"} (auth :pass))
                  _ (safe-click {:css "input.button-green"})
                  _ (replace-text {:css "#id_first_name"} (str (bio :first-name) " " (bio :last-name)))
                  _ (replace-text {:css "#id_external_url"} (bio :web))
                  _ (replace-text {:css "#id_biography"} (bio :bio))
                  _ (safe-click {:css "input.button-green"})
                  _ (quit)]
                 {:status "ok"})})))


(def updaters
  {:instagram update-instagram-bio
   :github update-github-bio
   :foursquare update-foursquare-bio
   :twitter update-twitter-bio})

(defn update-bio [config network]
  (if-let [updater (updaters network)]
    (updater config network)
    {network {:error "not supported"}}))

(defn -main [input-file output-file]
  (let [config (yaml/parse-string (slurp input-file))]
    (with-open [wrtr (writer output-file)]
      (.write wrtr
              (yaml/generate-string
                (doall (pmap #(update-bio config %)
                             (keys (:networks config)))))))
    (shutdown-agents)))

