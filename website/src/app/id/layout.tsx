import type { ReactNode } from "react";
import RootShell from "@/components/RootShell";
import "../globals.css";

export default function IdLayout({ children }: { children: ReactNode }) {
  return <RootShell lang="id">{children}</RootShell>;
}
