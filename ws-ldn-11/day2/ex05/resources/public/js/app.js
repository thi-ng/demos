var ex05 = {core:{}};
ex05.core.app = reagent.core.atom.call(null, cljs.core.PersistentArrayMap.EMPTY);
cljs.core.enable_console_print_BANG_.call(null);
ex05.core.update_worker_state = function(a) {
  var b = a.data, c = cljs.core.nth.call(null, b, 0, null);
  a = cljs.core.nth.call(null, b, 1, null);
  b = cljs.core.nth.call(null, b, 2, null);
  c = thi.ng.strf.core.timestamp.call(null) - c;
  a = new Uint8Array(a);
  a = new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null, "delta", "delta", 108939957), c, new cljs.core.Keyword(null, "buf-len", "buf-len", -1236020681), a.length, new cljs.core.Keyword(null, "msg", "msg", -1386103444), b], null);
  return cljs.core.swap_BANG_.call(null, ex05.core.app, cljs.core.assoc, new cljs.core.Keyword(null, "worker-msg", "worker-msg", 640171919), a);
};
ex05.core.start_worker = function() {
  (new cljs.core.Keyword(null, "worker", "worker", 938239996)).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null, ex05.core.app)).postMessage(cljs.core.pr_str.call(null, new cljs.core.PersistentArrayMap(null, 2, [new cljs.core.Keyword(null, "command", "command", -894540724), new cljs.core.Keyword(null, "start", "start", -355208981), new cljs.core.Keyword(null, "interval", "interval", 1708495417), 1E3], null)));
  return cljs.core.swap_BANG_.call(null, ex05.core.app, cljs.core.assoc, new cljs.core.Keyword(null, "worker-msg", "worker-msg", 640171919), null, new cljs.core.Keyword(null, "worker-active", "worker-active", -1372396502), !0);
};
ex05.core.stop_worker = function() {
  (new cljs.core.Keyword(null, "worker", "worker", 938239996)).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null, ex05.core.app)).postMessage(cljs.core.pr_str.call(null, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null, "command", "command", -894540724), new cljs.core.Keyword(null, "stop", "stop", -2140911342)], null)));
  return cljs.core.swap_BANG_.call(null, ex05.core.app, cljs.core.assoc, new cljs.core.Keyword(null, "worker-active", "worker-active", -1372396502), !1);
};
ex05.core.app_component = function() {
  var a = reagent.ratom.make_reaction.call(null, function() {
    return (new cljs.core.Keyword(null, "worker-msg", "worker-msg", 640171919)).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null, ex05.core.app));
  }), b = reagent.ratom.make_reaction.call(null, function(a) {
    return function() {
      return (new cljs.core.Keyword(null, "worker-active", "worker-active", -1372396502)).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null, ex05.core.app));
    };
  }(a));
  return function(a, b) {
    return function() {
      return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "div", "div", 1057191632), new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "h1", "h1", -1896887462), "Worker example"], null), cljs.core.truth_(cljs.core.deref.call(null, b)) ? new cljs.core.PersistentVector(null, 4, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "div", "div", 1057191632), 
      new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "p", "p", 151049309), "Latest message from worker:"], null), new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "p", "p", 151049309), cljs.core.truth_(cljs.core.deref.call(null, a)) ? new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "textarea", "textarea", -650375824), 
      new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null, "cols", "cols", -1914801295), 60, new cljs.core.Keyword(null, "rows", "rows", 850049680), 5, new cljs.core.Keyword(null, "value", "value", 305978217), cljs.core.pr_str.call(null, cljs.core.deref.call(null, a))], null)], null) : "Waiting..."], null), new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "p", "p", 151049309), new cljs.core.PersistentVector(null, 3, 5, 
      cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "button", "button", 1456579943), new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null, "on-click", "on-click", 1632826543), ex05.core.stop_worker], null), "Stop"], null)], null)], null) : new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "div", "div", 1057191632), new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, 
      "p", "p", 151049309), "Worker idle..."], null), new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null, "button", "button", 1456579943), new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null, "on-click", "on-click", 1632826543), ex05.core.start_worker], null), "Start"], null)], null)], null);
    };
  }(a, b);
};
ex05.core.init_app = function() {
  var a = new Worker("js/worker.js");
  a.onmessage = ex05.core.update_worker_state;
  return cljs.core.reset_BANG_.call(null, ex05.core.app, new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null, "worker", "worker", 938239996), a], null));
};
ex05.core.main = function() {
  ex05.core.init_app.call(null);
  return reagent.core.render_component.call(null, new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [ex05.core.app_component], null), document.body);
};
ex05.core.main.call(null);
