(ns engine.object
  (:require [clojure.set :as set]
            [engine.action :as action]
            [engine.entity :as entity]))

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

(defn make-object
  ([id location state description rule-type]
   (make-object id location state description rule-type {}))
  ([id location state description rule-type other]
   (merge (entity/make-entity
           id
           location
           state
           description
           #{:object})
          {:entity/rule-type rule-type}
          other)))

(def wooden-door
  (make-object "wooden-door" 0 {:open? false} "a wooden door" :object/wooden-door))

(def closed-door
  (make-object "closed-door" 1 {:open? false} "a wooden door" :object/wooden-door))

(def switched-off-lever-of-healing
  (make-object
   "lever-of-healing"
   0
   {:name "Lever of Healing"
    :action/additional-effects [{:action/type :action/heal
                                 :action/args {:action/target :self}}]
    :action/target {:entity/id "wooden-door"}
    :switched? false}
   "a glowing lever"
   :object/lever))

(def special-object
  (make-object
   "special-object"
   1
   {:action/target "toto"
    :action/effects [{:action/type :action/open
                      :action/args {:action/target {:entity/id 1}}}]}
   "something weird"
   :object/special))

(def boulgiboulga
  (make-object
   "boulgiboulga"
   1
   {}
   "a boulgiboulga"
   "runtime generated object 1"))

(def iron-door
  (make-object
   "iron-door"
   1
   {}
   "an iron door"
   :object/iron-door))

(def objects
  [switched-off-lever-of-healing special-object boulgiboulga iron-door wooden-door
   closed-door])
