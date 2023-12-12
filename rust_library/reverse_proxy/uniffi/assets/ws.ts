Deno.serve((req) => {
  if (req.headers.get("upgrade") == "websocket") {
    console.log("ws", req.url);
    const { socket, response } = Deno.upgradeWebSocket(req);
    socket.addEventListener("open", () => {
      console.log("a client connected!");
    });
    socket.addEventListener("message", (event) => {
      if (event.data === "ping") {
        socket.send("pong");
      } else {
        socket.send(event.data);
      }
    });
    return response;
  } else {
    console.log(req.method, req.url);
    return new Response(req.url, { status: 200 });
  }
});
