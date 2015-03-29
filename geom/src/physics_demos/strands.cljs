(ns physics-demos.strands
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.matrix :as mat :refer [M32 M44]]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.rect :as r]
   [thi.ng.geom.spatialtree :as accel]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.physics.core :as phys]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.domus.core :as dom]))

(defn attract
  [p q rsq strength delta]
  (let [d (g/- p (phys/position q))
        l (g/mag-squared d)]
    (if (< l rsq)
      (if (> l 0.0)
        (phys/add-force q (g/* d (/ (* (- 1.0 (/ l rsq)) (* strength delta))
                                    (Math/sqrt l))))))))

(defn accelerated-force-field
  [accel r strength]
  (let [rsq (* r r)]
    (fn [p delta]
      (let [p' (phys/position p)]
        (loop [neighbors (accel/select-with-circle @accel p' r)]
          (when-let [n (first neighbors)]
            (if-not (= p n) (attract p' n rsq strength delta))
            (recur (next neighbors))))))))

(defn make-strand
  [n fold offset]
  (let [particles (->> (range n)
                       (partition-all fold)
                       (map-indexed
                        (fn [i p]
                          (let [o (* i fold)]
                            (map #(g/+ offset (- % o) i) (if (odd? i) (reverse p) p))))))
        particles (mapv phys/particle (mapcat identity particles))
        springs (map (fn [[a b]] (phys/spring a b 1 1)) (partition 2 1 particles))]
    [particles springs]))

(defn init-physics
  [state n]
  (let [[particles1 springs1] (make-strand n 6 (vec2 6 9))
        [particles2 springs2] (make-strand n 6 (vec2 12 9))
        c1 {:c (phys/shape-constraint-inside (c/circle (vec2 9 17) 9))}
        c2 {:c (phys/shape-constraint-inside (c/circle (vec2 17 17) 9))}
        all-particles (concat particles1 particles2)
        accel (atom (accel/quadtree 0 0 32))
        ph (phys/physics
            {:particles all-particles
             :springs   (concat springs1 springs2)
             :behaviors {:g (phys/gravity (:gravity state))
                         :f (accelerated-force-field accel 1.5 -1)}
             :listeners {:iter (fn [physics _]
                                 (swap! accel
                                        (fn [_]
                                          (reduce
                                           #(g/add-point % (phys/position %2) %2)
                                           (g/clear* _)
                                           (:particles physics)))))}
             :drag 0.0})]
    (doseq [p particles1] (phys/add-constraints p c1))
    (doseq [p particles2] (phys/add-constraints p c2))
    (assoc state
           :physics ph
           :clusters [particles1 particles2])))

(defn strand-points
  [particles]
  (mapv #(phys/position %) particles))

(defn svg-strand
  [particles stroke fill]
  (let [pos (strand-points particles)]
    (svg/group {:stroke stroke}
               (svg/line-strip pos)
               (svg/group {:fill fill :stroke "none"} (map #(svg/circle % 0.2) pos)))))

(defn visualize-svg
  [{:keys [physics root] :as state}]
  (let [[c1 c2] (:clusters state)]
    (->> root
         (dom/clear!)
         (dom/create-dom!
          (svg/svg
           {:width 400 :height 400}
           (svg/group
            {:transform (g/scale M32 15) :stroke-width 0.1}
            (svg-strand c1 "#00f" "#0ff")
            (svg-strand c2 "#0f0" "#ff0")))))))

(defn -main
  []
  (let [state (atom (-> {:root (dom/by-id "app")
                         :gravity (vec2 0 0.025)}
                        (init-physics 100)))]
    (.log js/console state)
    (anim/animate
     (fn [[t frame]]
       ;;(.log js/console frame)
       (when (zero? (mod frame 200))
         (swap! state update :gravity g/-)
         (swap! state update :physics phys/add-behaviors {:g (phys/gravity (:gravity @state))}))
       (phys/timestep (:physics @state) 2)
       (visualize-svg @state)
       true))))

(-main)
