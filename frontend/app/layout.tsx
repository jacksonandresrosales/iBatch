import type { Metadata } from "next";
import { headers } from "next/headers";
import "./globals.css";

const description =
  "Control seguro y trazable para el procesamiento batch de transacciones financieras.";

export async function generateMetadata(): Promise<Metadata> {
  const requestHeaders = await headers();
  const host =
    requestHeaders.get("x-forwarded-host") ??
    requestHeaders.get("host") ??
    "localhost:3000";
  const protocol =
    requestHeaders.get("x-forwarded-proto") ??
    (host.includes("localhost") ? "http" : "https");
  const origin = `${protocol}://${host}`;

  return {
    title: {
      default: "iBatch | Operaciones financieras",
      template: "%s | iBatch",
    },
    description,
    openGraph: {
      title: "iBatch | Operaciones financieras",
      description,
      type: "website",
      url: origin,
      images: [
        {
          url: `${origin}/og.png`,
          width: 1536,
          height: 1024,
          alt: "iBatch, operaciones financieras seguras y trazables",
        },
      ],
    },
    twitter: {
      card: "summary_large_image",
      title: "iBatch | Operaciones financieras",
      description,
      images: [`${origin}/og.png`],
    },
  };
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
