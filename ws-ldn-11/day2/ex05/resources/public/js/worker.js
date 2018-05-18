var worker = {};
self.importScripts("base.js");
worker.state = cljs.core.atom.call(null, cljs.core.PersistentArrayMap.EMPTY);
worker.start_ping = function worker$start_ping(b) {
  setTimeout(function() {
    var c = (new Uint8Array(1E7)).buffer, d = thi.ng.strf.core.timestamp.call(null);
    self.postMessage([d, c, [cljs.core.str("Worker running ("), cljs.core.str(thi.ng.strf.core.now.call(null)), cljs.core.str(")")].join("")], [c]);
    return cljs.core.truth_((new cljs.core.Keyword(null, "active?", "active?", 459499776)).cljs$core$IFn$_invoke$arity$1(cljs.core.deref.call(null, worker.state))) ? worker$start_ping.call(null, b) : null;
  }, b);
  return cljs.core.swap_BANG_.call(null, worker.state, cljs.core.assoc, new cljs.core.Keyword(null, "active?", "active?", 459499776), !0);
};
worker.stop_ping = function() {
  return cljs.core.swap_BANG_.call(null, worker.state, cljs.core.assoc, new cljs.core.Keyword(null, "active?", "active?", 459499776), !1);
};
worker.dispatch_command = function(a) {
  a = cljs.reader.read_string.call(null, a.data);
  switch(cljs.core.keyword.call(null, (new cljs.core.Keyword(null, "command", "command", -894540724)).cljs$core$IFn$_invoke$arity$1(a)) instanceof cljs.core.Keyword ? cljs.core.keyword.call(null, (new cljs.core.Keyword(null, "command", "command", -894540724)).cljs$core$IFn$_invoke$arity$1(a)).fqn : null) {
    case "start":
      return worker.start_ping.call(null, (new cljs.core.Keyword(null, "interval", "interval", 1708495417)).cljs$core$IFn$_invoke$arity$1(a));
    case "stop":
      return worker.stop_ping.call(null);
    default:
      return console.warn([cljs.core.str("unknown worker command: "), cljs.core.str((new cljs.core.Keyword(null, "command", "command", -894540724)).cljs$core$IFn$_invoke$arity$1(a))].join(""));
  }
};
self.onmessage = worker.dispatch_command;
