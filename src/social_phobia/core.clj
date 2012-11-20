(ns social-phobia.core
  (:use [clj-webdriver.taxi]
        [clojure.java.io])
  (:require [clj-yaml.core :as yaml])
  (:gen-class main true))

(defmacro do-unless
  "
  Evaluates expression if `condition` which was called
  with result of previous expression returned false.

  Examples:
  =========

  (do-unless nil? (println 1) (println 2))
  1
  nil
  (do-unless nil? (do (println 1) 1) (println 2))
  1
  2
  nil"
  ([condition expr & exprs]
   `(let [r# ~expr]
      (if (~condition r#)
        r#
        (do-unless ~condition ~@exprs))))
  ([condition expr]
   expr))

(defn error [selector]
  {:status :fail :error (str (first (vals selector)) " not found")})

(defn- safe-find-element [selector f]
  "Find element, if element has been found call passed function,
  if not returh map with error."
  (let [el (find-element selector)]
    (if (:webelement el)
      (f el)
      (error selector))))

(defn- replace-text [selector text]
  (safe-find-element selector
                     #(-> %
                        clear
                        (input-text text))))

(defn- safe-click [selector]
  (safe-find-element selector #(click %)))

(defn- safe-send-keys [selector text]
  (safe-find-element selector #(send-keys % text)))

(defn- update [network bio f]
  "Run browser, pass auth map to function and return network map."
  (with-driver
    {:browser (bio :browser)}
    (let [auth (-> bio :networks network)]
      {network (f auth)})))

(defn update-twitter-bio [bio network]
  (update network bio (fn [auth]
                        (do-unless
                          :error
                          (to "http://twitter.com/")
                          (replace-text {:css "#signin-email"} (auth :login))
                          (replace-text {:css "#signin-password"} (auth :pass))
                          (safe-click {:css ".submit.flex-table-btn"})
                          (to "http://twitter.com/settings/profile")
                          (implicit-wait 3000)
                          (replace-text {:css "#user_name"} (str (bio :first-name)
                                                                 " "
                                                                 (bio :last-name)))
                          (replace-text {:css "#user_location"} (bio :location))
                          (replace-text {:css "#user_url"} (bio :web))
                          (replace-text {:css "#user_description"} (bio :bio))
                          (safe-click {:css "#settings_save"})
                          (quit)
                          {:status "ok"}))))

(defn update-foursquare-bio [bio network]
  (update network bio (fn [auth]
                        (do-unless
                          :error
                          (to "https://foursquare.com/settings/")
                          (replace-text {:css "#username"} (auth :login))
                          (replace-text {:css "#password"} (auth :pass))
                          (safe-click {:css "input.greenButton"})
                          (if-let [avatar-path (bio :avatar-path)]
                            (send-keys {:xpath "//li[@id='profPic']//input"} avatar-path))
                          (replace-text {:css "#firstname"} (bio :first-name))
                          (replace-text {:css "#lastname"} (bio :last-name))
                          (replace-text {:css "#userEmail"} (bio :email))
                          (replace-text {:css "textarea.formStyle"} (bio :bio))
                          (replace-text {:css "#ht_id"} (bio :location))
                          (safe-click {:css "input.greenButton"})
                          (quit)
                          {:status "ok"}))))

(defn update-github-bio [bio network]
  (update network bio (fn [auth]
                        (do-unless
                          :error
                          (to "https://github.com/login")
                          (replace-text {:css "#login_field"} (auth :login))
                          (replace-text {:css "#password"} (auth :pass))
                          (safe-click {:xpath "//input[@type='submit']"})
                          (to "https://github.com/settings/profile")
                          (replace-text {:xpath "//dl[@data-name='profile_name']//input"}
                                        (str (bio :first-name) " " (bio :last-name)))
                          (replace-text {:xpath "//dl[@data-name='profile_email']//input"} (bio :email))
                          (replace-text {:xpath "//dl[@data-name='profile_blog']//input"} (bio :web))
                          (replace-text {:xpath "//dl[@data-name='profile_company']//input"} (bio :company))
                          (replace-text {:xpath "//dl[@data-name='profile_location']//input"} (bio :location))
                          (replace-text {:xpath "//dl[@data-name='gravatar_email']//input"} (bio :email))
                          (safe-click {:css "button.button.classy.primary"})
                          (quit)
                          {:status "ok"}))))

(defn update-instagram-bio [bio network]
  (update network bio (fn [auth]
                        (do-unless
                          :error
                          (to "https://instagram.com/accounts/login/?next=/accounts/edit/")
                          (replace-text {:css "#id_username"} (auth :login))
                          (replace-text {:css "#id_password"} (auth :pass))
                          (safe-click {:css "input.button-green"})
                          (replace-text {:css "#id_first_name"} (str (bio :first-name) " " (bio :last-name)))
                          (replace-text {:css "#id_external_url"} (bio :web))
                          (replace-text {:css "#id_biography"} (bio :bio))
                          (safe-click {:css "input.button-green"})
                          (quit)
                          {:status "ok"}))))

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

