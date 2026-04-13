(ns engine.action)

(defn make-show-effect
  [args]
  {::type :show
   ::args args})

(defn make-mutation-effect
  "Creates a mutation action structure.
   Args:
     target: map with object id, typically {::object/id id}
     args: map of arguments for the mutation
   Returns: map with mutation type, target, and args"
  [target args]
  {::type :mutation
   ::target target
   ::args args})

(defn make-effect-list
  [effects]
  {::effects (flatten (remove nil? (if (sequential? effects) effects [effects])))})

(comment
  ;; open -> closed -> open
  ;; secret -> closed -> closed -> open
  ;; secret -> closed -> secret
  :end)
