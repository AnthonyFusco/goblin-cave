(ns engine.action
  (:require [engine.action.command :as command]))

(defn make-show-effect
  [args]
  {:action/type :show
   :action/args args})

(defn make-mutation-effect
  [target args]
  {:action/type :action/mutation
   :action/target target
   :action/mutation args})

(defn make-effects
  ([]
   {:action/effects '()})
  ([effects]
   {:action/effects (flatten (remove nil? (if (sequential? effects) effects [effects])))}))

(defn make-commands
  ([]
   {:action/commands '()})
  ([commands]
   {:action/commands (flatten (remove nil? (if (sequential? commands) commands [commands])))}))

(defn make-effects-and-commands
  ([]
   {:action/effects '()
    :action/commands '()})
  ([{:keys [effects commands]}]
   {:action/commands (flatten (remove nil? (if (sequential? commands) commands [commands])))
    :action/effects (flatten (remove nil? (if (sequential? effects) effects [effects])))}))

(defn get-effect-list
  [effects]
  (or (:action/effects effects) []))

(defn get-command-list
  [commands]
  (or (:action/commands commands) []))

(defn make-action
  ([type]
   {:action/type type
    :action/args {}})
  ([type args]
   {:action/type type
    :action/args args}))

(def action-rules
  {:action/default
   {:actions
    {:action/advance (fn [{:keys [instance]}]
                       (let [{:keys [id action/target]} instance]
                         (command/teleport id target)))}}})

