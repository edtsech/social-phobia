(ns social-phobia.core
  (:use [clj-webdriver.taxi]
        [clojure.java.io])
  (:require [clj-yaml.core :as yaml])
  (:gen-class main true))

(defn- replace-text
  "Find an input and replace a text in it.

  Examples:
  =========

  (replace-text \"input#field\" \"smith\")"
  [selector text]
  (-> (find-element selector)
    clear
    (input-text text)))

(defn update-twitter-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "http://twitter.com/")
               (let [auth (-> bio :networks :twitter)]
                 (input-text "#signin-email" (auth :login))
                 (input-text "#signin-password" (auth :pass)))
               (click ".submit.flex-table-btn")
               (to "http://twitter.com/settings/profile")
               (implicit-wait 3000)
               (replace-text {:css "#user_name"} (str (bio :first-name) " " (bio :last-name)))
               (replace-text {:css "#user_location"} (bio :location))
               (replace-text {:css "#user_url"} (bio :web))
               (replace-text {:css "#user_description"} (bio :bio))
               (click "#settings_save")
               (quit))
  {:twitter "ok"})

(defn update-foursquare-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "https://foursquare.com/login?continue=%2F&clicked=true")
               (let [auth (-> bio :networks :foursquare)]
                 (input-text "#username" (auth :login))
                 (input-text "#password" (auth :pass)))
               (click (find-element {:css "input.greenButton"}))
               (to "https://foursquare.com/settings/")
               ; (implicit-wait 3000)
               (replace-text {:css "#firstname"} (bio :first-name))
               (replace-text {:css "#lastname"} (bio :last-name))
               (replace-text {:css "#userEmail"} (bio :email))
               (replace-text {:css "textarea.formStyle"} (bio :bio))
               (replace-text {:css "#ht_id"} (bio :location))
               (click (find-element {:css "input.greenButton"}))
               (quit))
  {:foursquare "ok"})

(defn update-github-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "https://github.com/login")
               (let [auth (-> bio :networks :github)]
                 (input-text "#login_field" (auth :login))
                 (input-text "#password" (auth :pass)))
               (click (find-element {:xpath "//input[@type='submit']"}))
               (to "https://github.com/settings/profile")
               (replace-text {:xpath "//dl[@data-name='profile_name']//input"}
                             (str (bio :first-name) " " (bio :last-name)))
               (replace-text {:xpath "//dl[@data-name='profile_email']//input"} (bio :email))
               (replace-text {:xpath "//dl[@data-name='profile_blog']//input"} (bio :web))
               (replace-text {:xpath "//dl[@data-name='profile_company']//input"} (bio :company))
               (replace-text {:xpath "//dl[@data-name='profile_location']//input"} (bio :location))
               (replace-text {:xpath "//dl[@data-name='gravatar_email']//input"} (bio :email))
               (click (find-element {:css "button.button.classy.primary"}))
               (quit))
  {:github "ok"})

(defn update-instagram-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "https://instagram.com/accounts/login/?next=/accounts/edit/")
               (let [auth (-> bio :networks :instagram)]
                 (input-text "#id_username" (auth :login))
                 (input-text "#id_password" (auth :pass)))
               (click (find-element {:css "input.button-green"}))
               ; (implicit-wait 3000)
               (replace-text {:css "#id_first_name"} (str (bio :first-name) " " (bio :last-name)))
               (replace-text {:css "#id_external_url"} (bio :web))
               (replace-text {:css "#id_biography"} (bio :bio))
               (click (find-element {:css "input.button-green"}))
               (quit))
  {:instagram "ok"})

(def updaters
  {:instagram update-instagram-bio
   :github update-github-bio
   :foursquare update-foursquare-bio
   :twitter update-twitter-bio})

(defn update-bio [config network]
  (if-let [updater (updaters network)]
    (updater config)
    {network "not supported"}))

(defn- write-to [file-name text]
  (with-open [wrtr (writer file-name)]
    (.write wrtr text)))

(defn -main [input-file output-file]
  (let [config (yaml/parse-string (slurp input-file))]
    (write-to output-file
              (yaml/generate-string (map #(update-bio config %)
                                         (keys (:networks config)))))))

