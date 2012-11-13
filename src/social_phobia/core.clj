(ns social-phobia.core
  (:use [clj-webdriver.taxi])
  (:require [clj-yaml.core :as yaml]))

(defn- replace-text [id s]
  (-> (find-element {:id id})
    clear
    (input-text s)))

(defn update-twitter-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "http://twitter.com/")
               (input-text "#signin-email" (-> bio :networks :twitter :login))
               (input-text "#signin-password" (-> bio :networks :twitter :pass))
               (click ".submit.flex-table-btn")

               (to "http://twitter.com/settings/profile")
               (implicit-wait 3000)

               (replace-text "user_name" (str (bio :first-name) " " (bio :last-name)))
               (replace-text "user_location" (bio :location))
               (replace-text "user_url" (bio :web))
               (replace-text "user_description" (bio :bio))

               (click "#settings_save")

               (quit))
  {:twitter "ok"})

(defn update-foursquare-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "https://foursquare.com/login?continue=%2F&clicked=true")
               (input-text "#username" (-> bio :networks :foursquare :login))
               (input-text "#password" (-> bio :networks :foursquare :pass))
               (click (find-element {:css "input.greenButton"}))
               (to "https://foursquare.com/settings/")
               ; (implicit-wait 3000)
               (replace-text "firstname" (bio :first-name))
               (replace-text "lastname" (bio :last-name))
               (replace-text "userEmail" (bio :email))
               (-> (find-element {:css "textarea.formStyle"})
                 clear
                 (input-text (bio :bio)))
               (replace-text "ht_id" (bio :location))
               (click (find-element {:css "input.greenButton"}))
               (quit))
  {:foursquare "ok"})

(defn update-github-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "https://github.com/login")
               (input-text "#login_field" (-> bio :networks :github :login))
               (input-text "#password" (-> bio :networks :github :pass))
               (click (find-element {:xpath "//input[@type='submit']"}))
               (to "https://github.com/settings/profile")
               (-> (find-element {:xpath "//dl[@data-name='profile_name']//input"})
                 clear
                 (input-text (str (bio :first-name) " " (bio :last-name))))
               (-> (find-element {:xpath "//dl[@data-name='profile_email']//input"})
                 clear
                 (input-text (bio :email)))
               (-> (find-element {:xpath "//dl[@data-name='profile_blog']//input"})
                 clear
                 (input-text (bio :web)))
               (-> (find-element {:xpath "//dl[@data-name='profile_company']//input"})
                 clear
                 (input-text (bio :company)))
               (-> (find-element {:xpath "//dl[@data-name='profile_location']//input"})
                 clear
                 (input-text (bio :location)))
               (-> (find-element {:xpath "//dl[@data-name='gravatar_email']//input"})
                 clear
                 (input-text (bio :email)))
               (click (find-element {:css "button.button.classy.primary"}))
               (quit))
  {:github "ok"})

(defn update-instagram-bio [bio]
  (with-driver {:browser (bio :browser)}
               (to "https://instagram.com/accounts/login/?next=/accounts/edit/")
               (input-text "#id_username" (-> bio :networks :instagram :login))
               (input-text "#id_password" (-> bio :networks :instagram :pass))
               (click (find-element {:css "input.button-green"}))
               ; (implicit-wait 3000)
               (replace-text "id_first_name" (str (bio :first-name) " " (bio :last-name)))
               (replace-text "id_external_url" (bio :web))
               (replace-text "id_biography" (bio :bio))
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

(defn -main [file-name]
  (let [config (yaml/parse-string (slurp file-name))]
    (println (yaml/generate-string (map #(update-bio config %)
                                        (keys (:networks config)))))))

