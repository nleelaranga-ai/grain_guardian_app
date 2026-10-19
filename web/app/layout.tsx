import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "GrainGuardian — Central Storage Intelligence & Warehouse Monitoring",
  description: "Real-time thermal stratification, hotspot tracking, and paddy health dashboard for warehouse godowns.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-grain-bg text-grain-textPrimary antialiased">
        {children}
      </body>
    </html>
  );
}
