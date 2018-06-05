
; Hi. I'm ***COMPLETELY NEW*** to clojure. As such, I may not have done everything in a "standard" way, and I may not be following best practices (typically, I really like to know what best practices are, but under these circumstances I did not have time). I used the home.clj file as a template for this search page, and then I googled pretty much everything else. Enjoy the mess!

(ns my-exercise.search
  (:require [hiccup.page :refer [html5]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [clj-http.client :as client]
            [clojure.string :as str]
            [clojure.edn :as edn]
            ))

(defn header []
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1.0, maximum-scale=1.0"}]
   [:title "Next election results"]
   [:link {:rel "stylesheet" :href "default.css"}]])

; this is the starting url with which I will make an api call
(def starting-url "https://api.turbovote.org/elections/upcoming")

; making sure city is lower-cased, and that any spaces in it are converted to underscore
(defn format-place-for-url[city]
  (when city (str/lower-case(clojure.string/replace city " " "_"))))

; making sure state is lower-cased
(defn format-state-for-url[state]
  (when state (str/lower-case state)))

; a hacky way to "error-check". Clearly this could be improved if I end up learning clojure properly.
(defn error-check [paramslist]
 (if (empty? ((paramslist :params) :state))
  (throw (AssertionError. "Please go back, and try again. You must enter a valid State!"))
  )
 (if (empty? ((paramslist :params) :city))
  (throw (AssertionError. "Please go back, and try again. You must enter a valid city!")))
 )

(defn params->address-map [paramslist]
 (:params paramslist)
 )

(defn address-map->ocd-id-list [addmap]
 (let [{:keys [city state]} addmap
  city (format-place-for-url city)
  state (format-state-for-url state)
  ocdidlist [(when state(str "ocd-division/country:us/state:" state))
  (when (and state city) (str "ocd-division/country:us/state:" state "/place:" city))]
  ocdidlist (remove nil? ocdidlist)
    ; ocdidlist (filter (complement nil?) ocdidlist)
  ]
  ocdidlist
  ))

(defn election-query [ocdidlist]
 (client/get starting-url {:query-params { :district-divisions (str/join "," ocdidlist) }}))

(defn results [req]
 (-> req
  params->address-map
  address-map->ocd-id-list
  election-query
  )
 )

; this function will create the final url string with the city and the state providing they pass the error check.

; this is the search results, and also where I make the api call
(defn search [election-result]
  ; here I'm getting the body off of the response
  (def election-query-body (get election-result :body))
  ; I had little success with reading json. so I stuck to the edn parser.
  (def parsed-elections (edn/read-string election-query-body))

  ; this shows the voter the info they just searched for.
  ; Currently, only the city and state are relevant form fields. It would make sense to either make this search function more robust, OR remove street and zip code fields from the form.
  [:div
    [:h1 "Election Search Results for:"]
     ; [:h4 "Address Entered "]
     ; [:p "street: " ((paramslist :params) :street) ]
     ; [:p "street-2: " ((paramslist :params) :street-2) ]
     ; [:p "city: " ((paramslist :params) :city) ]
     [:p "state: " ((paramslist :params) :state) ]
     ; [:p "zip: " ((paramslist :params) :zip) ]
     [:br]
     [:h2 "Upcoming Elections at Address "]
     ; logic for a location with no elections coming up:
        (if (empty? parsed-elections)
        [:p "Sorry, there are no upcoming elections at that address :/"]
        [:div (for [election parsed-elections]
          [:div [:h3 (:description election)]
          [:li (election :date)]
          ; this is an actual link to an external page
          [:li [:a {:href (str (election :polling-place-url-shortened))} (str (election :polling-place-url-shortened))]]])])])

; this page structure is also borrowed from the home page.
; I first check for errors, then I preform my long search function with the params passed in as the "request" object
(defn page [request]
  (html5
    (header)
    ; (error-check request)
    (search (results request) ) ))

;; Here's a list of the things I'd like to improve upon:
    ; allowing for a zip code look up
    ; allowing for a street look up
    ; allowing for partial look ups
    ; better styling
    ; better error handling
    ; grabbing more info to display from the api call
    ; adding a back button







