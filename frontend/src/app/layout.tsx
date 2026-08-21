import type { Metadata } from "next";
import "./globals.css";
import styles from "./page.module.css";

export const metadata: Metadata = {
  title: "HYF Final Project",
  description: "HackYourFuture final project",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>
        <div className={styles.page}>
          <main className={styles.main}>{children}</main>
        </div>
      </body>
    </html>
  );
}
