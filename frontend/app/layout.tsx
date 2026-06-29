import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Voca – Master Your Vocabulary",
  description: "A premium spaced-repetition vocabulary learning workspace with AI enrichment, quizzes, and smart review scheduling."
};

export default function RootLayout({
  children
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <meta name="theme-color" content="#0f0b1e" />
        <meta name="color-scheme" content="dark" />
      </head>
      <body>{children}</body>
    </html>
  );
}
