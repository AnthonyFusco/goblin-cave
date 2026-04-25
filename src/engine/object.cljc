(ns engine.object
  (:require [clojure.set :as set]
            [engine.action :as action]))

(defn extend-object-rules
  [base extra]
  (let [update-fn (fn [base key]
                    (update-in base [key] set/union (get extra key)))]
    (-> base
        (update-fn :semantics)
        (update-fn :intrinsic)
        (update-fn :actions)
        (merge (dissoc extra :semantics :intrinsic :actions)))))

(defn door-rules
  [extra]
  (extend-object-rules
   {:semantics #{:semantic/exit}
    :actions {:action/activate
              (fn [{:keys [instance]}]
                (let [{:keys [:entity/state :entity/id]} instance]
                  (action/make-mutation-effect
                   {:entity/id id}
                   {:open? (not (:open? state))})))}}
   extra))

(defn lever-default-self-mutation
  [id state]
  {:pre [(contains? state :switched?)]}
  (action/make-mutation-effect {:entity/id id}
                               {:switched? (not (:switched? state))}))

(defn lever-activate-handler
  [{:keys [:entity/state :entity/id]}]
  (let [self-effect (lever-default-self-mutation id state)
        show-effect (action/make-show-effect {:description "You heard a click somewhere"})]
    [self-effect show-effect]))

(def object-rules
  {:object/wooden-door
   (door-rules
    {:name "wooden door"
     :intrinsic #{:intrinsic/flammable}
     :actions   {:action/open (fn [_] (action/make-show-effect {:describe "You push open the creaky wooden door"}))
                 :action/close (fn [_] (action/make-show-effect {:describe "You close the creaky wooden door"}))}})

   :object/door
   (door-rules {})

   :object/lever
   {:semantics #{:semantic/activable}
    :actions {:action/activate (fn [{:keys [instance]}]
                                 (let [{:keys [onActivate]} instance
                                       onActivate (or onActivate lever-activate-handler)]
                                   (onActivate instance)))}}

   "runtime generated object 1"
   {:name "boulgiboulga"
    :semantics #{:semantic/food}
    :intrinsic #{:intrinsic/poisoned}}

   :object/iron-door
   (door-rules
    {:name "iron door"
     :actions   {:action/open (fn [{:keys [name]}] (action/make-show-effect {:describe (str "You heave the " name " open")}))}})})

(def closed-door
  {:entity/id "wooden-door"
   :entity/rule-type :object/wooden-door
   :entity/state {:exit-to 1
                  :open? false}})

(def switched-off-lever-of-healing
  {:entity/id "lever-of-healing"
   :entity/rule-type :object/lever
   :entity/state {:name "Lever of Healing"
                  :action/additional-effects [{:action/type :action/heal
                                               :action/args {:action/target :self}}]
                  :action/target {:entity/id "wooden-door"}
                  :switched? false}})

(def special-object
  {:entity/id "special-object"
   :entity/rule-type :object/special
   :entity/state {:action/target "toto"
                  :action/effects [{:action/type :action/open
                                    :action/args {:action/target {:entity/id 1}}}]}})

(def boulgiboulga
  {:entity/id "boulgiboulga"
   :entity/rule-type "runtime generated object 1"})

(def iron-door
  {:entity/id "iron-door"
   :entity/rule-type :object/iron-door})

(defn make-object
  ([id rule-type state]
   (make-object id rule-type state {}))
  ([id rule-type state other]
   (merge {:entity/id id
           :entity/rule-type rule-type
           :entity/state state}
          other)))

(def wooden-door
  (make-object "wooden-door" :object/wooden-door {:open? false}))

(def objects
  [closed-door switched-off-lever-of-healing special-object boulgiboulga iron-door wooden-door])
