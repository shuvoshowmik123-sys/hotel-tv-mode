import type { Metadata } from "next";
import { DM_Sans, DM_Mono } from "next/font/google";
import { LenisProvider } from "../components/LenisProvider";
import "./globals.css";

const dmSans = DM_Sans({
  subsets: ["latin"],
  variable: "--font-sans",
});

const dmMono = DM_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "Central Admin Panel",
  description: "Hotel operations portal",
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
    apple: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${dmSans.variable} ${dmMono.variable} antialiased min-h-screen`}>
        <LenisProvider>
          {children}
        </LenisProvider>
      </body>
    </html>
  );
}
