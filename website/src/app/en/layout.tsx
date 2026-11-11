import type { ReactNode } from "react";
import RootShell from "@/components/RootShell";
import "../globals.css";

export default function EnLayout({ children }: { children: ReactNode }) {
  return <RootShell lang="en">{children}</RootShell>;
}
