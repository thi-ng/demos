(ns physics-strands.core
  (:require
   [thi.ng.math.core :as m]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.matrix :as mat :refer [M32 M44]]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.spatialtree :as accel]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.physics.core :as phys]
   [thi.ng.geom.gl.webgl.animator :refer [animate]]
   [thi.ng.domus.core :as dom]))

(defn attract!
  "Takes a 2d or 3d attractor position, a particle and attractor
  params (squared radius, strength, time delta). If particle is within
  attraction radius, applies proportional strength force to particle.
  If strength is negative, particle will be repelled."
  [p q rsq strength delta]
  (let [d (m/- p (phys/position q))
        l (+ (m/mag-squared d) 1e-6)]
    (if (< l rsq)
      (phys/add-force
       q (m/* d (/ (* (- 1.0 (/ l rsq)) (* strength delta))
                   (Math/sqrt l)))))))

(defn accelerated-force-field
  "Takes a mutable quadtree or octree, an attraction radius and strength.
  Returns a function which will be applied as behavior to all
  particles to create a force field around each. The spatial tree is
  used to limit k-neighbor lookups to only particles within the given
  radius around each particle."
  [accel r strength]
  (let [rsq (* r r)]
    (fn [p delta]
      (let [p' (phys/position p)]
        (loop [neighbors (accel/select-with-circle accel p' r)]
          (when-let [n (first neighbors)]
            (if-not (= p n) (attract! p' n rsq strength delta))
            (recur (next neighbors))))))))

(defn update-accelerator
  "Takes a mutable quadtree or octree and returns a function to be
  used as simulation listener. When called, updates the tree to
  reflect current positions of all particles in the physics sim."
  [accel]
  (fn [physics _]
    (reduce
     #(g/add-point % (phys/position %2) %2)
     (g/clear! accel)
     (:particles physics))))

(defn make-strand
  "Creates a strand of spring-connected 2d particles in a zigzag order
  along the Y axis. The first arg defines the total number of
  particles in the strand, the second how many particles per row. The
  last arg defines the 2d start position. Returns 2-elem vector
  of [particles springs]."
  [n fold offset]
  (let [particles (->> (range n)
                       (partition-all fold)
                       (map-indexed
                        (fn [i p]
                          (let [o (* i fold)]
                            (map #(m/+ offset (- % o) i) (if (odd? i) (reverse p) p))))))
        particles (mapv phys/particle (mapcat identity particles))
        springs   (map (fn [[a b]] (phys/spring a b 0.5 1)) (partition 2 1 particles))]
    [particles springs]))

(defn init-physics
  "Takes a state map and integer defining number of particles per
  strand. First creates two strands, each with its own circular
  constraint. Then defines full VerletPhysics setup with gravity and
  force field behaviors. Also attaches a simulation listener to keep
  particle quadtree in sync. Returns updated state map w/ physics
  related data injected."
  [state n]
  (let [[p1 s1] (make-strand n 6 (vec2 7 2))
        [p2 s2] (make-strand n 6 (vec2 12 2))
        c1      {:c (phys/shape-constraint-inside (c/circle (vec2 10 10) 9))}
        c2      {:c (phys/shape-constraint-inside (c/circle (vec2 17 10) 9))}
        accel   (accel/quadtree 0 0 26)]
    (doseq [p p1] (phys/add-constraints p c1))
    (doseq [p p2] (phys/add-constraints p c2))
    (assoc state
           :physics  (phys/physics
                      {:particles (concat p1 p2)
                       :springs   (concat s1 s2)
                       :behaviors {:g (phys/gravity (:gravity state))
                                   :f (accelerated-force-field accel 1 -1)}
                       :listeners {:iter (update-accelerator accel)}})
           :clusters [p1 p2])))

(defn particle-positions
  "Takes a seq of particles, returns vector of their positions."
  [particles]
  (mapv #(phys/position %) particles))

(defn svg-strand
  "Takes a seq of particles and stroke/fill colors, returns a SVG
  group defining particles as circles and a polyline between
  particles."
  [particles stroke fill]
  (let [pos (particle-positions particles)]
    (svg/group
     {:stroke stroke}
     (svg/line-strip pos)
     (svg/group
      {:fill fill :stroke "none"}
      (sequence (map #(svg/circle % 0.2)) pos)))))

(defn visualize-svg
  "Takes a state map and visualizes the current state of the physic
  sim as SVG DOM element."
  [{:keys [physics root svg-attrs] :as state}]
  (let [[c1 c2] (:clusters state)]
    (->> root
         (dom/clear!)
         (dom/create-dom!
          (svg/svg
           {:width 400 :height 300}
           (svg/group
            svg-attrs
            (svg-strand c1 "#00f" "#0ff")
            (svg-strand c2 "#0f0" "#ff0")))))))

(defn -main
  []
  (let [state (-> {:root (dom/by-id "app")
                   :gravity (vec2 0 0.025)
                   :svg-attrs {:transform (g/scale M32 15) :stroke-width 0.1}}
                  (init-physics 100)
                  (atom))]
    (animate
     (fn [t frame]
       ;; randomize gravity direction every N frames
       (when (zero? (mod frame 120))
         (swap! state assoc :gravity (v/randvec2 0.025))
         (swap! state update :physics phys/add-behaviors {:g (phys/gravity (:gravity @state))}))
       (phys/timestep (:physics @state) 2)
       (visualize-svg @state)
       true))))

(-main)
