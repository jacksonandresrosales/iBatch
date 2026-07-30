import assert from "node:assert/strict";
import test from "node:test";

async function render(pathname = "/files/available") {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request(`http://localhost${pathname}`, {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("renderiza la pantalla de archivos disponibles", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>iBatch \| Operaciones financieras<\/title>/i);
  assert.match(html, /Archivos disponibles/);
  assert.match(html, /Procesamiento protegido desde el origen/);
  assert.match(html, /transactions_30072026\.csv/);
  assert.doesNotMatch(html, /codex-preview|react-loading-skeleton/i);
});
