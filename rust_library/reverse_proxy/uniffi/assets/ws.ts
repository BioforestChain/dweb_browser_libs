const encoder = new TextEncoder();
Deno.serve(async (req) => {
  if (req.headers.get("upgrade") == "websocket") {
    console.log("ws", req.url);
    const { socket, response } = Deno.upgradeWebSocket(req);
    socket.addEventListener("open", () => {
      console.log("a client connected!");
    });
    socket.addEventListener("message", (event) => {
      console.log("ws on message", event.data);
      if (event.data === "ping") {
        socket.send("pong");
      } else {
        socket.send(event.data);
      }
    });
    return response;
  } else {
    console.log(req.method, req.url);
    const res_body = new ReadableStream<Uint8Array>({
      async start(controller) {
        controller.enqueue(encoder.encode(`url=${req.url} `));
        const ti = setTimeout(() => {
          console.log(`size=${size} by timeout`);
          // close("timeout");
        }, 5000);
        let size = 0;
        const close = (reason: string) => {
          try {
            controller.enqueue(encoder.encode(`size=${size} by ${reason}`));
            console.log(req.url, req.body);
            controller.close();
            clearTimeout(ti);
          } catch {
            console.warn("closed");
          }
        };

        if (req.body) {
          const reader = req.body.getReader();
          while (true) {
            const item = await reader.read();
            if (item.done) {
              break;
            }
            size += item.value.length;
          }
        }
        close("request-end");
      },
    });
    console.log(req.url, req.body);
    return new Response(res_body, { status: 200 });
  }
});

/**
```
b = new Uint8Array(4e7);
b.forEach((_,i)=>b[i]=i);
fetch('https://localhost:1443',{method:"POST",body:b}).then(res=>res.text()).then(console.info)
```

```
ws = new WebSocket('wss://localhost:1443');
ws.onopen = console.info;
ws.onmessage = (e)=>console.info(e.data);
ws.onerror = console.error
ws.onclose = console.warn

// after open
ws.send(new Uint8Array(4e7))
```
 */
