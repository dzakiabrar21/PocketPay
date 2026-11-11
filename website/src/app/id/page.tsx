import Landing from "@/components/Landing";
import { id } from "@/content/id";
import { buildMetadata } from "@/lib/metadata";

export const metadata = buildMetadata(id, "/id");

export default function Page() {
  return <Landing dict={id} />;
}
