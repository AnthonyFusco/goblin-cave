(ns engine.object
  (:require [clojure.set :as set]
            [engine.action :as action]
            [engine.semantic :as semantic]))

(defn extend-object-rules
  "Merges base object rules with extra rules by unioning sets.
   Args:
     base: base rules map with :semantics, :intrinsic, :actions sets
     extra: additional rules to merge
   Returns: merged rules map"
  [base extra]
  (let [update-fn (fn [base key]
                    (update-in base [key] set/union (get extra key)))]
    (-> base
        (update-fn :semantics)
        (update-fn :intrinsic)
        (update-fn :actions)
        (merge (dissoc extra :semantics :intrinsic :actions)))))

(defn door-rules
  "Creates door-specific rules by extending with exit semantics.
   Args:
     extra: additional door rules to merge
   Returns: door rules map with exit semantics"
  [extra]
  (extend-object-rules {:semantics #{::semantic/exit}} extra))

(defn lever-default-self-mutation
  "Creates a self-mutation for lever state toggle.
   Args:
     id: object id of the lever
     state: map with required :switched? key (boolean)
   Returns: mutation action that toggles :switched? state"
  [id state]
  {:pre [(contains? state :switched?)]}
  (action/make-mutation-effect {::id id}
                               (assoc {:switched? (not (:switched? state))}
                                      :meta "self-mutation => lever switched state")))

(defn lever-activate-handler
  "Handler for lever activation with state toggling.

   Args:

     - instance: map with :engine.action/state and ::object/id keys

   Returns: map with combined activation and self-mutation effects"
  [{:keys [:engine.action/state ::id]}]
  (let [self-effect (lever-default-self-mutation id state)
        show-effect (action/make-show-effect {:description "You heard a click somewhere"})]
    [self-effect show-effect]))

(def object-rules
  {::wooden-door
   (door-rules
    {:name "wooden door"
     :intrinsic #{:intrinsic/flammable}
     :actions   {::action/open (fn [_] (action/make-show-effect {:describe "You push open the creaky wooden door"}))
                 ::action/close (fn [_] (action/make-show-effect {:describe "You close the creaky wooden door"}))}})

   ::door
   (door-rules {})

   ::lever
   {:semantics #{::semantic/activable}
    :actions {::action/activate (fn [{:keys [instance]}]
                                  (let [{:keys [onActivate]} instance
                                        onActivate (or onActivate lever-activate-handler)]
                                    (onActivate instance)))}}

   "runtime generated object 1"
   {:name "boulgiboulga"
    :semantics #{::semantic/food}
    :intrinsic #{:intrinsic/poisoned}}

   ::iron-door
   (door-rules
    {:name "iron door"
     :actions   {::action/open (fn [{:keys [name]}] (action/make-show-effect {:describe (str "You heave the " name " open")}))}})})