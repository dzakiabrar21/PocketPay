import Landing from "@/components/Landing";
import { en } from "@/content/en";
import { buildMetadata } from "@/lib/metadata";

export const metadata = buildMetadata(en, "/");

export default function Page() {
  return <Landing dict={en} />;
}
