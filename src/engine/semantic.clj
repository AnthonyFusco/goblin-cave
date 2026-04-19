(ns engine.semantic
  (:require [engine.action :as action]))

(defn compute-full-effects
  "Computes full effects list, optionally preventing defaults.
   Args:
     state: map that may contain :engine.action/prevent-default?
     options: map with :default-effects, :additional-effects, :self-effects keys
   Returns: flattened sequence of applicable effects"
  [state {:keys [default-effects additional-effects self-effects]}]
  (let [prevent-default? (boolean (:action/prevent-default? state))]
    (remove nil? (flatten [(if prevent-default? [] default-effects) self-effects additional-effects]))))

(defn default-activate-action
  "Creates a default activation action.
   Args:
     state: map with required :engine.action/target key
   Returns: activate action map with state as args"
  [{:keys [action/target] :as state}]
  {:pre [(contains? state :action/target)]}
  {:action/type :action/activate
   :action/args {:action/target target
                  :meta "default activate action"}})

(defn make-effect
  "Make effect. no op for now.

   Args:

     - effect: map with :engine.action/type and :engine.action/args

   Returns: effect with state merged into args"
  [effect]
  (let [type (:action/type effect)
        args (:action/args effect)]
    {:action/type type :action/args args}))

(defn make-effects
  "Make effects.
   Args:
     effects: sequence of effect maps
   Returns: sequence of effects"
  [effects]
  (map make-effect effects))

(defn make-additional-effects
  "Extracts and processes additional effects from state.
   Args:
     state: map that may contain :engine.action/additional-effects key
   Returns: sequence of effects with state merged, or empty sequence"
  [state]
  (make-effects (get state :action/additional-effects [])))

(defn activate-default-handler
  "Default handler for activation actions.
   Args:
     instance: map with :engine.action/state key
   Returns: map with :engine.action/effects sequence"
  [{:keys [entity/state]}]
  (let [additional-effects (make-additional-effects state)
        default-effects (default-activate-action state)
        self-effects []
        effects (compute-full-effects state {:default-effects default-effects
                                             :additional-effects additional-effects
                                             :self-effects self-effects})]
    effects))

(def semantic-rules
  {:semantic/exit
   {:action/open (fn [{:keys [name instance]}]
                    (let [{:keys [entity/state]} instance]
                      (if (= state :open)
                        (action/make-show-effect {:describe "Already opened."})
                        [(action/make-show-effect {:describe (str "You open the " name)})
                         (action/make-mutation-effect instance {:state :open
                                                                :meta "door state mutation => open"})])))

    :action/close (fn [{:keys [instance]}]
                     (let [{:keys [entity/state]} instance]
                       (if (= state :open)
                         (action/make-show-effect {:describe "You close the door"
                                                   :state :closed})
                         (action/make-show-effect {:describe "Already closed"}))))}

   :semantic/activable
   {:action/activate (fn [{:keys [instance]}]
                        (let [{:keys [onActivate]} instance
                              onActivate (or onActivate activate-default-handler)]
                          (onActivate instance)))}

   :semantic/food
   {:action/eat (fn [{:keys [name]}] (action/make-show-effect {:describe (str "Nom! No more " name " :(")}))}

   #{:semantic/food :intrinsic/poisoned}
   {:action/eat (fn [{:keys [name]}] (action/make-show-effect {:describe (str "Ouch! The " name " is poisoned :(")}))}

   :intrinsic/flammable
   {:action/burn (fn [{:keys [name action-params]}]
                    (let [{:keys [color]} action-params]
                      (action/make-show-effect {:describe (str "The " name " bursts into " color (when color " ") "flames!")})))}})