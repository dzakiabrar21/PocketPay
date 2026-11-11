import type { Dict } from "@/content/types";
import Architecture from "./Architecture";
import Docs from "./Docs";
import Effects from "./Effects";
import Features from "./Features";
import Footer from "./Footer";
import Gallery from "./Gallery";
import Hero from "./Hero";
import Nav from "./Nav";
import QuickStart from "./QuickStart";
import Rules from "./Rules";
import Security from "./Security";
import Story from "./Story";
import Testing from "./Testing";

const techs = [
  "Kotlin",
  "Jetpack Compose",
  "Room",
  "SQLCipher",
  "Hilt",
  "WorkManager",
  "CameraX",
  "ML Kit",
  "Android Keystore",
  "BiometricPrompt",
  "QRIS · EMVCo",
  "Robolectric",
];

/** The whole page. Rendered once per language by `/` and `/en`. */
export default function Landing({ dict }: { dict: Dict }) {
  return (
    <>
      <a className="skip" href="#main">
        {dict.skip}
      </a>
      <Nav dict={dict} />
      <main id="main">
        <Hero dict={dict} />
        <div className="band" aria-hidden="true">
          <div className="track">
            {[...techs, ...techs].map((t, i) => (
              <span key={`${t}${i}`}>{t}</span>
            ))}
          </div>
        </div>
        <Story dict={dict} />
        <Features dict={dict} />
        <Gallery dict={dict} />
        <Rules dict={dict} />
        <Security dict={dict} />
        <Architecture dict={dict} />
        <Testing dict={dict} />
        <QuickStart dict={dict} />
        <Docs dict={dict} />
      </main>
      <Footer dict={dict} />
      <Effects />
    </>
  );
}
