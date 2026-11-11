import type { Dict } from "@/content/types";
import { REPO_URL } from "./Nav";

export default function Footer({ dict }: { dict: Dict }) {
  return (
    <footer>
      <div className="wrap">
        <div className="big">PocketPay</div>
        <div className="row">
          <div>{dict.footer.text}</div>
          <div>
            <a href={REPO_URL} target="_blank" rel="noopener noreferrer">
              github.com/dzakiabrar21/PocketPay
            </a>
          </div>
        </div>
      </div>
    </footer>
  );
}
