(ns engine.semantic
  (:require [engine.action :as action]))

(defn compute-full-effects
  "Computes full effects list, optionally preventing defaults.
   Args:
     state: map that may contain :engine.action/prevent-default?
     options: map with :default-effects, :additional-effects, :self-effects keys
   Returns: flattened sequence of applicable effects"
  [state {:keys [default-effects additional-effects self-effects]}]
  (let [prevent-default? (boolean (:engine.action/prevent-default? state))]
    (remove nil? (flatten [(if prevent-default? [] default-effects) self-effects additional-effects]))))

(defn default-activate-action
  "Creates a default activation action.
   Args:
     state: map with required :engine.action/target key
   Returns: activate action map with state as args"
  [{:keys [:engine.action/target] :as state}]
  {:pre [(contains? state :engine.action/target)]}
  {:engine.action/type :engine.action/activate
   :engine.action/args {:engine.action/target target
                        :meta "default activate action"}})

(defn make-effect
  "Make effect. no op for now.

   Args:

     - effect: map with :engine.action/type and :engine.action/args

   Returns: effect with state merged into args"
  [effect]
  (let [type (:engine.action/type effect)
        args (:engine.action/args effect)]
    {:engine.action/type type :engine.action/args args}))

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
  (make-effects (get state :engine.action/additional-effects [])))

(defn activate-default-handler
  "Default handler for activation actions.
   Args:
     instance: map with :engine.action/state key
   Returns: map with :engine.action/effects sequence"
  [{:keys [:engine.action/state]}]
  (let [additional-effects (make-additional-effects state)
        default-effects (default-activate-action state)
        self-effects []
        effects (compute-full-effects state {:default-effects default-effects
                                             :additional-effects additional-effects
                                             :self-effects self-effects})]
    effects))

(def semantic-rules
  {::exit
   {::action/open (fn [{:keys [name instance]}]
                    (let [{:keys [:engine.action/state]} instance]
                      (if (= state :open)
                        (action/make-show-effect {:describe "Already opened."})
                        [(action/make-show-effect {:describe (str "You open the " name)})
                         (action/make-mutation-effect instance {:state :open
                                                                :meta "door state mutation => open"})])))

    ::action/close (fn [{:keys [instance]}]
                     (let [{:keys [:state]} instance]
                       (if (= state :open)
                         (action/make-show-effect {:describe "You close the door"
                                                   :state :closed})
                         (action/make-show-effect {:describe "Already closed"}))))}

   ::activable
   {::action/activate (fn [{:keys [instance]}]
                        (let [{:keys [onActivate]} instance
                              onActivate (or onActivate activate-default-handler)]
                          (onActivate instance)))}

   ::food
   {::action/eat (fn [{:keys [name]}] (action/make-show-effect {:describe (str "Nom! No more " name " :(")}))}

   #{::food :intrinsic/poisoned}
   {::action/eat (fn [{:keys [name]}] (action/make-show-effect {:describe (str "Ouch! The " name " is poisoned :(")}))}

   :intrinsic/flammable
   {::action/burn (fn [{:keys [name action-params]}]
                    (let [{:keys [color]} action-params]
                      (action/make-show-effect {:describe (str "The " name " bursts into " color (when color " ") "flames!")})))}})